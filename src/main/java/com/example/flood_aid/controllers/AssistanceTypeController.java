package com.example.flood_aid.controllers;

import com.example.flood_aid.models.AssistanceType;
import com.example.flood_aid.services.AssistanceTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assistanceTypes")
public class AssistanceTypeController {

    private final AssistanceTypeService assistanceTypeService;

    public AssistanceTypeController(AssistanceTypeService assistanceTypeService) {
        this.assistanceTypeService = assistanceTypeService;
    }

    @GetMapping
    public ResponseEntity<List<AssistanceType>> getAssistanceTypes() {
        return ResponseEntity.ok(assistanceTypeService.getAssistanceTypes());
    }

    @GetMapping("/all")
    public ResponseEntity<List<AssistanceType>> getAllAssistanceTypes() {
        return ResponseEntity.ok(assistanceTypeService.getAllAssistanceTypes());
    }

    @PostMapping
    public ResponseEntity<AssistanceType> createAssistanceType(@RequestBody AssistanceType request) {
        return ResponseEntity.ok(assistanceTypeService.createAssistanceType(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssistanceType> updateAssistanceType(@PathVariable Long id,
            @RequestBody AssistanceType request) {
        return ResponseEntity.ok(assistanceTypeService.updateAssistanceType(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssistanceType(@PathVariable Long id) {
        assistanceTypeService.deleteAssistanceType(id);
        return ResponseEntity.noContent().build();
    }
}
