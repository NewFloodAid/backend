package com.example.flood_aid.services;

import com.example.flood_aid.models.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
public class MapService {

    private static final int TILE_SIZE = 256;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${osm.static.zoom:18}")
    private int osmZoom;

    @Value("${osm.static.width:1024}")
    private int osmWidth;

    @Value("${osm.static.height:768}")
    private int osmHeight;

    @Value("${osm.static.tile.url.template:https://tile.openstreetmap.org/%d/%d/%d.png}")
    private String osmTileUrlTemplate;

    @Value("${osm.static.user-agent:FloodAid/1.0 (+https://openstreetmap.org)}")
    private String osmUserAgent;

    public byte[] generateStaticMapWithPin(Location location, Long reportId) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return null;
        }

        try {
            int zoom = Math.max(0, Math.min(osmZoom, 19));
            int width = Math.max(320, Math.min(osmWidth, 1600));
            int height = Math.max(240, Math.min(osmHeight, 1600));

            BufferedImage mapImage = renderOsmStaticMap(
                    location.getLatitude(),
                    location.getLongitude(),
                    zoom,
                    width,
                    height,
                    reportId);

            if (mapImage == null) {
                return null;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(mapImage, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to generate OSM map for report {}: {}", reportId, e.getMessage());
            return null;
        }
    }

    public String buildOpenStreetMapLink(Location location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return null;
        }

        int zoom = Math.max(0, Math.min(osmZoom, 19));
        return String.format(
                Locale.US,
                "https://www.openstreetmap.org/?mlat=%.7f&mlon=%.7f#map=%d/%.7f/%.7f",
                location.getLatitude(),
                location.getLongitude(),
                zoom,
                location.getLatitude(),
                location.getLongitude());
    }

    private BufferedImage renderOsmStaticMap(
            double latitude,
            double longitude,
            int zoom,
            int width,
            int height,
            Long reportId) {

        double centerPixelX = longitudeToPixelX(longitude, zoom);
        double centerPixelY = latitudeToPixelY(latitude, zoom);

        double topLeftPixelX = centerPixelX - (width / 2.0);
        double topLeftPixelY = centerPixelY - (height / 2.0);

        int startTileX = (int) Math.floor(topLeftPixelX / TILE_SIZE);
        int startTileY = (int) Math.floor(topLeftPixelY / TILE_SIZE);
        int endTileX = (int) Math.floor((topLeftPixelX + width) / TILE_SIZE);
        int endTileY = (int) Math.floor((topLeftPixelY + height) / TILE_SIZE);

        int tileCount = 1 << zoom;

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        try {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
                if (tileY < 0 || tileY >= tileCount) {
                    continue;
                }

                for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                    int wrappedTileX = Math.floorMod(tileX, tileCount);
                    byte[] tileBytes = fetchTile(zoom, wrappedTileX, tileY, reportId);
                    if (tileBytes == null || tileBytes.length == 0) {
                        continue;
                    }

                    BufferedImage tileImage;
                    try {
                        tileImage = ImageIO.read(new ByteArrayInputStream(tileBytes));
                    } catch (IOException e) {
                        log.warn(
                                "OSM tile image read error for report {} (z={}, x={}, y={}): {}",
                                reportId,
                                zoom,
                                wrappedTileX,
                                tileY,
                                e.getMessage());
                        continue;
                    }
                    if (tileImage == null) {
                        continue;
                    }

                    int drawX = (int) Math.round((tileX * TILE_SIZE) - topLeftPixelX);
                    int drawY = (int) Math.round((tileY * TILE_SIZE) - topLeftPixelY);
                    graphics.drawImage(tileImage, drawX, drawY, TILE_SIZE, TILE_SIZE, null);
                }
            }

            drawPin(graphics, width / 2.0, height / 2.0);
            return canvas;
        } finally {
            graphics.dispose();
        }
    }

    private byte[] fetchTile(int zoom, int tileX, int tileY, Long reportId) {
        String tileUrl = String.format(Locale.US, osmTileUrlTemplate, zoom, tileX, tileY);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tileUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", osmUserAgent)
                    .header("Accept", "image/png,image/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            log.warn(
                    "OSM tile request failed for report {} (z={}, x={}, y={}) with status {}",
                    reportId,
                    zoom,
                    tileX,
                    tileY,
                    response.statusCode());
        } catch (Exception e) {
            log.warn(
                    "OSM tile request error for report {} (z={}, x={}, y={}): {}",
                    reportId,
                    zoom,
                    tileX,
                    tileY,
                    e.getMessage());
        }

        return null;
    }

    private double longitudeToPixelX(double longitude, int zoom) {
        double worldSize = TILE_SIZE * Math.pow(2, zoom);
        return (longitude + 180.0) / 360.0 * worldSize;
    }

    private double latitudeToPixelY(double latitude, int zoom) {
        double clippedLatitude = Math.max(Math.min(latitude, 85.05112878), -85.05112878);
        double latitudeRad = Math.toRadians(clippedLatitude);
        double worldSize = TILE_SIZE * Math.pow(2, zoom);
        return (1.0 - Math.log(Math.tan(latitudeRad) + (1.0 / Math.cos(latitudeRad))) / Math.PI) / 2.0 * worldSize;
    }

    private void drawPin(Graphics2D graphics, double centerX, double centerY) {
        // Scale from the same 40x54 shape used in frontend Leaflet pin icon.
        double scale = 0.55;
        double offsetX = centerX - (20.0 * scale);
        double offsetY = centerY - (54.0 * scale);

        Path2D.Double pinShape = new Path2D.Double();
        pinShape.moveTo(offsetX + (20.0 * scale), offsetY);
        pinShape.curveTo(
                offsetX + (8.954 * scale),
                offsetY,
                offsetX,
                offsetY + (8.954 * scale),
                offsetX,
                offsetY + (20.0 * scale));
        pinShape.curveTo(
                offsetX,
                offsetY + (34.779 * scale),
                offsetX + (17.25 * scale),
                offsetY + (51.942 * scale),
                offsetX + (19.21 * scale),
                offsetY + (53.835 * scale));
        pinShape.curveTo(
                offsetX + (19.66 * scale),
                offsetY + (54.27 * scale),
                offsetX + (20.34 * scale),
                offsetY + (54.27 * scale),
                offsetX + (20.79 * scale),
                offsetY + (53.835 * scale));
        pinShape.curveTo(
                offsetX + (22.75 * scale),
                offsetY + (51.942 * scale),
                offsetX + (40.0 * scale),
                offsetY + (34.779 * scale),
                offsetX + (40.0 * scale),
                offsetY + (20.0 * scale));
        pinShape.curveTo(
                offsetX + (40.0 * scale),
                offsetY + (8.954 * scale),
                offsetX + (31.046 * scale),
                offsetY,
                offsetX + (20.0 * scale),
                offsetY);
        pinShape.closePath();

        graphics.setColor(new Color(239, 83, 80));
        graphics.fill(pinShape);

        graphics.setColor(Color.WHITE);
        int innerRadius = (int) Math.round(7 * scale);
        int innerCenterX = (int) Math.round(offsetX + (20.0 * scale));
        int innerCenterY = (int) Math.round(offsetY + (20.0 * scale));
        graphics.fillOval(
                innerCenterX - innerRadius,
                innerCenterY - innerRadius,
                innerRadius * 2,
                innerRadius * 2);
    }
}
