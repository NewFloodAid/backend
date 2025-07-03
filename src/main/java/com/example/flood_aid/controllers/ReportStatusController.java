package com.example.flood_aid.controllers;


import com.example.flood_aid.models.ReportStatus;
import com.example.flood_aid.services.ReportStatusService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportStatuses")
@AllArgsConstructor
public class ReportStatusController {
    ReportStatusService reportStatusService;

    @GetMapping
    public ResponseEntity<List<ReportStatus>> getReportStatuses(@RequestParam(value = "isUser") Boolean isUser) {
        return ResponseEntity.ok(reportStatusService.getReportStatuses(isUser));
    }
}
