package com.example.flood_aid.controllers;

import com.example.flood_aid.configs.JwtPrincipal;
import com.example.flood_aid.configs.JwtUtil;
import com.example.flood_aid.models.Admin;
import com.example.flood_aid.models.RefreshToken;
import com.example.flood_aid.models.dto.auth.*;
import com.example.flood_aid.repositories.RefreshTokenRepository;
import com.example.flood_aid.services.AdminService;
import com.example.flood_aid.services.AuditLogService;
import com.example.flood_aid.services.LineAuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private static final Pattern HEX_32_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

    private final AdminService adminService;
    private final LineAuthService lineAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final String lineChannelId;

    public AuthController(
            AdminService adminService,
            LineAuthService lineAuthService,
            JwtUtil jwtUtil,
            RefreshTokenRepository refreshTokenRepository,
            AuditLogService auditLogService,
            @Value("${line.channel.id}") String lineChannelId) {
        this.adminService = adminService;
        this.lineAuthService = lineAuthService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.lineChannelId = lineChannelId;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Optional<Admin> optionalAdmin = adminService.findByUsername(loginRequest.getUsername());

        if (optionalAdmin.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }

        Admin admin = optionalAdmin.get();

        // Check if account is locked
        if (admin.isLocked()) {
            return ResponseEntity.status(423).body(Map.of(
                    "message", "Account is locked. Try again later.",
                    "lockedUntil", admin.getLockedUntil().toString()));
        }

        // Check if account is active
        if (!admin.getIsActive()) {
            return ResponseEntity.status(403).body(Map.of("message", "Account is deactivated"));
        }

        // Validate password with BCrypt
        if (!adminService.validatePassword(loginRequest.getPassword(), admin.getPasswordHash())) {
            adminService.handleFailedLogin(admin);
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }

        // Successful login
        adminService.handleSuccessfulLogin(admin);

        List<Long> districtIds = adminService.getDistrictIdsForAdmin(admin.getId());

        // Generate tokens
        String accessToken = jwtUtil.generateToken(
                admin.getUsername(), "WEB", admin.getId(), admin.getRole().name(), districtIds);
        String refreshToken = jwtUtil.generateRefreshToken(admin.getUsername(), admin.getId());

        // Save refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .admin(admin)
                .tokenHash(refreshToken.substring(refreshToken.length() - 32)) // Last 32 chars as identifier
                .expiresAt(new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L))
                .deviceInfo(request.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // Audit log
        auditLogService.log(admin.getId(), "LOGIN", "ADMIN", admin.getId(), request.getRemoteAddr());

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(admin.getRole().name())
                .fullName(admin.getFullName())
                .districtIds(districtIds)
                .build();

        return ResponseEntity.ok(response);
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

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid refresh token"));
            }

            String tokenType = jwtUtil.extractTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid token type"));
            }

            String username = jwtUtil.extractUsername(refreshToken);

            Optional<Admin> optionalAdmin = adminService.findByUsername(username);
            if (optionalAdmin.isEmpty() || !optionalAdmin.get().getIsActive()) {
                return ResponseEntity.status(401).body(Map.of("message", "Admin account not found or inactive"));
            }

            Admin admin = optionalAdmin.get();
            List<Long> districtIds = adminService.getDistrictIdsForAdmin(admin.getId());

            String newAccessToken = jwtUtil.generateToken(
                    admin.getUsername(), "WEB", admin.getId(), admin.getRole().name(), districtIds);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "role", admin.getRole().name(),
                    "fullName", admin.getFullName(),
                    "districtIds", districtIds));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        JwtPrincipal principal = getCurrentJwtPrincipal();
        if (principal != null && principal.adminId() != null) {
            refreshTokenRepository.revokeAllByAdminId(principal.adminId());
            auditLogService.log(principal.adminId(), "LOGOUT", "ADMIN", principal.adminId(), request.getRemoteAddr());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin() {
        JwtPrincipal principal = getCurrentJwtPrincipal();
        if (principal == null || principal.adminId() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        Optional<Admin> optionalAdmin = adminService.findById(principal.adminId());
        if (optionalAdmin.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Admin not found"));
        }

        Admin admin = optionalAdmin.get();
        var districts = adminService.getDistrictsForAdmin(admin.getId());

        AdminProfileResponse profile = AdminProfileResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .phone(admin.getPhone())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .lastLoginAt(admin.getLastLoginAt())
                .assignedDistricts(districts.stream()
                        .map(ad -> AdminProfileResponse.DistrictInfo.builder()
                                .id(ad.getDistrict().getId())
                                .nameInThai(ad.getDistrict().getNameInThai())
                                .nameInEnglish(ad.getDistrict().getNameInEnglish())
                                .build())
                        .toList())
                .build();

        return ResponseEntity.ok(profile);
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

    private JwtPrincipal getCurrentJwtPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        return null;
    }
}
