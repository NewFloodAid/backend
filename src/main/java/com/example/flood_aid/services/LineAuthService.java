package com.example.flood_aid.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class LineAuthService {

    private static final String LINE_VERIFY_URL = "https://api.line.me/oauth2/v2.1/verify";

    public Optional<String> verifyAndExtractUserId(String idToken, String clientId) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8) +
                                 "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    LINE_VERIFY_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<?, ?> body = response.getBody();
                Object sub = body != null ? body.get("sub") : null;
                if (sub instanceof String lineUserId && !lineUserId.isBlank()) {
                    return Optional.of(lineUserId);
                }
                log.warn("LINE token verification succeeded but subject claim was missing.");
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LINE ID Token verification failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
