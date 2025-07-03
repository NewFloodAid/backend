package com.example.flood_aid.controllers;


import com.example.flood_aid.models.AssistanceType;
import com.example.flood_aid.services.AssistanceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistanceTypes")
@RequiredArgsConstructor
public class AssistanceTypeController {

    @Autowired
    AssistanceTypeService assistanceTypeService;

    @GetMapping
    public ResponseEntity<List<AssistanceType>> getAssistanceTypes() {
        return ResponseEntity.ok(assistanceTypeService.getAssistanceTypes());
    }

}
