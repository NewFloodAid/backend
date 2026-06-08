package com.example.flood_aid.services;

import com.example.flood_aid.models.AssistanceType;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import com.example.flood_aid.models.District;
import com.example.flood_aid.repositories.DistrictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssistanceTypeService {

    @Autowired
    private AssistanceTypeRepository assistanceTypeRepository;

    @Autowired
    private DistrictRepository districtRepository;

    public List<AssistanceType> getAssistanceTypes(Long districtId) {
        if (districtId != null) {
            List<AssistanceType> types = assistanceTypeRepository.findByDistrictIdAndIsActiveTrueOrderByIdAsc(districtId);
            if (types.isEmpty()) {
                // Auto-clone globals
                return cloneGlobalsForDistrict(districtId, true);
            }
            return types;
        }
        return assistanceTypeRepository.findByDistrictIsNullOrderByIdAsc().stream().filter(AssistanceType::getIsActive).collect(Collectors.toList());
    }

    public List<AssistanceType> getAllAssistanceTypes(Long districtId) {
        if (districtId != null) {
            List<AssistanceType> types = assistanceTypeRepository.findByDistrictIdOrderByIdAsc(districtId);
            if (types.isEmpty()) {
                // Auto-clone globals
                return cloneGlobalsForDistrict(districtId, false);
            }
            return types;
        }
        return assistanceTypeRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    private List<AssistanceType> cloneGlobalsForDistrict(Long districtId, boolean activeOnly) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "District not found"));
        
        List<AssistanceType> globals = assistanceTypeRepository.findByDistrictIsNullOrderByIdAsc();
        List<AssistanceType> cloned = globals.stream().map(g -> {
            AssistanceType clone = new AssistanceType();
            clone.setName(g.getName());
            clone.setIsActive(g.getIsActive());
            clone.setExtraFieldLabel(g.getExtraFieldLabel());
            clone.setExtraFieldPlaceholder(g.getExtraFieldPlaceholder());
            clone.setExtraFieldRequired(g.getExtraFieldRequired());
            clone.setDistrict(district);
            return assistanceTypeRepository.save(clone);
        }).collect(Collectors.toList());

        if (activeOnly) {
            return cloned.stream().filter(AssistanceType::getIsActive).collect(Collectors.toList());
        }
        return cloned;
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
        
        if (request.getDistrict() != null && request.getDistrict().getId() != null) {
            District district = districtRepository.findById(request.getDistrict().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "District not found"));
            assistanceType.setDistrict(district);
        }

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
