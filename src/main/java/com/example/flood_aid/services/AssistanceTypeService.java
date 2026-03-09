package com.example.flood_aid.services;

import com.example.flood_aid.models.AssistanceType;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class AssistanceTypeService {

    @Autowired
    private AssistanceTypeRepository assistanceTypeRepository;

    public List<AssistanceType> getAssistanceTypes() {
        return assistanceTypeRepository.findByIsActiveTrueOrderByIdAsc();
    }

    public List<AssistanceType> getAllAssistanceTypes() {
        return assistanceTypeRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public AssistanceType createAssistanceType(AssistanceType request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assistance type name is required");
        }

        AssistanceType assistanceType = new AssistanceType();
        assistanceType.setName(name);
        assistanceType.setIsActive(Boolean.TRUE.equals(request.getIsActive()) || request.getIsActive() == null);
        assistanceType.setExtraFieldLabel(sanitizeOptional(request.getExtraFieldLabel()));
        assistanceType.setExtraFieldPlaceholder(sanitizeOptional(request.getExtraFieldPlaceholder()));
        assistanceType.setExtraFieldRequired(Boolean.TRUE.equals(request.getExtraFieldRequired()));

        return assistanceTypeRepository.save(assistanceType);
    }

    @Transactional
    public AssistanceType updateAssistanceType(Long id, AssistanceType request) {
        AssistanceType assistanceType = assistanceTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistance type not found"));

        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assistance type name is required");
        }

        assistanceType.setName(name);
        if (request.getIsActive() != null) {
            assistanceType.setIsActive(request.getIsActive());
        }
        assistanceType.setExtraFieldLabel(sanitizeOptional(request.getExtraFieldLabel()));
        assistanceType.setExtraFieldPlaceholder(sanitizeOptional(request.getExtraFieldPlaceholder()));
        assistanceType.setExtraFieldRequired(Boolean.TRUE.equals(request.getExtraFieldRequired()));

        return assistanceTypeRepository.save(assistanceType);
    }

    @Transactional
    public void deleteAssistanceType(Long id) {
        AssistanceType assistanceType = assistanceTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistance type not found"));
        assistanceType.setIsActive(false);
        assistanceTypeRepository.save(assistanceType);
    }

    private String sanitizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
