package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AdminRole {
    SUPER_ADMIN,
    DISTRICT_ADMIN;

    @JsonCreator
    public static AdminRole fromString(String value) {
        return AdminRole.valueOf(value.toUpperCase());
    }
}
