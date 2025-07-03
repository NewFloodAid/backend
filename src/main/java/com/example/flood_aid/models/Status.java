package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    PENDING,
    PROCESS,
    SUCCESS,
    REJECTED;

    @JsonCreator
    public static Status fromString(String value) {
        return Status.valueOf(value.toUpperCase());
    }
}
