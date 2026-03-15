package com.example.flood_aid.configs;

import java.util.UUID;

public record JwtPrincipal(String username, String appType, UUID userId) {
}
