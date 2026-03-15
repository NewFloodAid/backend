package com.example.flood_aid.controllers;

import com.example.flood_aid.configs.JwtUtil;
import com.example.flood_aid.models.UserAdmin;
import com.example.flood_aid.services.LineAuthService;
import com.example.flood_aid.services.UserAdminService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Pattern HEX_32_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private LineAuthService lineAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${line.channel.id}")
    private String lineChannelId;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        UserAdmin userAdmin = userAdminService.findByUsername(username);

        if (userAdmin == null) {
            return ResponseEntity.status(400).body("Invalid username or password");
        }

        if (!userAdminService.validatePassword(password, userAdmin.getPassword())) {
            return ResponseEntity.status(400).body("Invalid username or password");
        }

        String token = jwtUtil.generateToken(username, "WEB");
        return ResponseEntity.ok(Map.of("jwtToken", token));
    }

    @PostMapping("/line-login")
    public ResponseEntity<?> verifyLineIdToken(@RequestHeader(value = "IDToken", required = true) String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "IDToken header is required.", "status", "error"));
        }

        Optional<String> lineUserId = lineAuthService.verifyAndExtractUserId(idToken, lineChannelId);
        if (lineUserId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid LINE IDToken.", "status", "error"));
        }

        UUID normalizedUserId;
        try {
            normalizedUserId = toUuidFromLineUserId(lineUserId.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid LINE user identity.", "status", "error"));
        }

        String jwtToken = jwtUtil.generateToken(lineUserId.get(), "LIFF", normalizedUserId);

        return ResponseEntity.ok(Map.of("jwtToken", jwtToken, "status", "success"));
    }

    private UUID toUuidFromLineUserId(String lineUserId) {
        String normalized = lineUserId != null && lineUserId.startsWith("U")
                ? lineUserId.substring(1)
                : lineUserId;

        if (normalized == null || !HEX_32_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid LINE user id format.");
        }

        String uuidString = normalized.replaceFirst(
                "^([a-fA-F0-9]{8})([a-fA-F0-9]{4})([a-fA-F0-9]{4})([a-fA-F0-9]{4})([a-fA-F0-9]{12})$",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(uuidString);
    }
}
