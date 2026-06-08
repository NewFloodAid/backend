package com.example.flood_aid.configs;

import java.util.List;
import java.util.UUID;

public record JwtPrincipal(
    String username,
    String appType,
    UUID userId,
    String role,
    Long adminId,
    List<Long> districtIds
) {
    // Backward-compatible constructor for LIFF users
    public JwtPrincipal(String username, String appType, UUID userId) {
        this(username, appType, userId, null, null, List.of());
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }

    public boolean isDistrictAdmin() {
        return "DISTRICT_ADMIN".equals(role);
    }

    public boolean isWebUser() {
        return "WEB".equalsIgnoreCase(appType);
    }

    public boolean isLiffUser() {
        return "LIFF".equalsIgnoreCase(appType);
    }
}
