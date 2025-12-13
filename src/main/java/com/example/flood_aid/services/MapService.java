package com.example.flood_aid.services;

import com.example.flood_aid.models.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
public class MapService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Value("${google.maps.map.id:}")
    private String googleMapsMapId;

    @Value("${google.maps.zoom:19}")
    private int googleMapsZoom;

    @Value("${google.maps.width:640}")
    private int googleMapsWidth;

    @Value("${google.maps.height:480}")
    private int googleMapsHeight;

    @Value("${google.maps.scale:2}")
    private int googleMapsScale;

    @Value("${google.maps.maptype:roadmap}")
    private String googleMapsMapType;

    @Value("${google.maps.styles:}")
    private String googleMapsStyles;

    public byte[] generateStaticMapWithPin(Location location, Long reportId) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return null;
        }

        byte[] googleImage = fetchGoogleStaticMap(location, reportId);
        if (googleImage == null || googleImage.length == 0) {
            log.warn("Failed to fetch Google Static Map for report {}. Skipping map rendering.", reportId);
        }
        return googleImage;
    }

    private byte[] fetchGoogleStaticMap(Location location, Long reportId) {
        if (googleMapsApiKey == null || googleMapsApiKey.isBlank()) {
            log.warn("Google Maps API key not configured; cannot fetch Google map for report {}", reportId);
            return null;
        }
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        int zoom = Math.max(0, Math.min(googleMapsZoom, 21));
        int baseWidth = Math.max(1, Math.min(googleMapsWidth, 640));
        int baseHeight = Math.max(1, Math.min(googleMapsHeight, 640));
        int scale = Math.max(1, Math.min(googleMapsScale, 2));
        try {
            String markerParam = URLEncoder.encode(
                    String.format(Locale.US, "color:0xFF8C00|size:mid|%f,%f", lat, lon),
                    StandardCharsets.UTF_8);
            String mapIdParam = googleMapsMapId == null || googleMapsMapId.isBlank()
                    ? ""
                    : "&map_id=" + URLEncoder.encode(googleMapsMapId, StandardCharsets.UTF_8);
            StringBuilder styleBuilder = new StringBuilder();
            if (googleMapsStyles != null && !googleMapsStyles.isBlank()) {
                String[] styles = googleMapsStyles.split(";");
                for (String style : styles) {
                    String trimmed = style.trim();
                    if (!trimmed.isEmpty()) {
                        styleBuilder.append("&style=").append(URLEncoder.encode(trimmed, StandardCharsets.UTF_8));
                    }
                }
            }
            String mapType = URLEncoder.encode(googleMapsMapType != null ? googleMapsMapType : "roadmap",
                    StandardCharsets.UTF_8);
            String url = String.format(Locale.US,
                    "https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=%d&size=%dx%d&scale=%d&maptype=%s%s%s&markers=%s&key=%s",
                    lat, lon, zoom, baseWidth, baseHeight, scale, mapType, mapIdParam, styleBuilder.toString(),
                    markerParam,
                    URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "FloodAid-PDF/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] body = response.body();
                if (body != null && body.length > 0) {
                    return body;
                }
                log.warn("Google Static Map response body was empty for report {}", reportId);
            } else {
                log.warn("Google Static Map request failed for report {} with status {}", reportId,
                        response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Google Static Map for report {} (lat={}, lon={}): {}", reportId, lat, lon,
                    e.getMessage());
        }
        return null;
    }
}
