package com.example.flood_aid.controllers;

import com.example.flood_aid.models.UserAdmin;
import com.example.flood_aid.services.LineAuthService;
import com.example.flood_aid.services.UserAdminService;
import com.example.flood_aid.configs.JwtUtil;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

    String token = jwtUtil.generateToken(username, "Web");
    return ResponseEntity.ok(Map.of("jwtToken", token));
}

    @PostMapping("/line-login")
    public ResponseEntity<?> verifyLineIdToken(@RequestHeader(value = "IDToken", required = true) String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "IDToken header is required.", "status", "error"));
        }

        boolean isValid = lineAuthService.verifyIdToken(idToken, lineChannelId);
        if (!isValid) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid LINE IDToken.", "status", "error"));
        }

        String jwtToken = jwtUtil.generateToken("LINE_USER", "LIFF");

        return ResponseEntity.ok(Map.of("jwtToken", jwtToken, "status", "success"));
    }

}
