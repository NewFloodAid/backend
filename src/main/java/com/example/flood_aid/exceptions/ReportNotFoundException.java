package com.example.flood_aid.exceptions;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long id) {
        super("Report not found with ID: " + id);
    }
}

