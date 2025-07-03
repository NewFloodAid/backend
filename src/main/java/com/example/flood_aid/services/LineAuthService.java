package com.example.flood_aid.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class LineAuthService {

    private static final String LINE_VERIFY_URL = "https://api.line.me/oauth2/v2.1/verify";

    public boolean verifyIdToken(String idToken, String clientId) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            System.out.println("Verifying ID Token: " + idToken);
            System.out.println("Using Client ID: " + clientId);

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

                System.out.println("LINE Response: " + response.getBody());
                return true;
            } else {

                System.err.println("LINE ID Token verification failed: " + response.getBody());
                return false;
            }
        } catch (Exception e) {

            System.err.println("LINE ID Token verification failed: " + e.getMessage());
            return false;
        }
    }
}
