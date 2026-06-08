package com.example.flood_aid.controllers;

import com.example.flood_aid.models.District;
import com.example.flood_aid.repositories.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictRepository districtRepository;

    @GetMapping
    public List<Map<String, Object>> getDistrictsByProvince(@RequestParam(required = false) String province) {
        List<District> districts;
        if (province != null && !province.isBlank()) {
            districts = districtRepository.findByProvinceNameInThaiOrderByNameInThaiAsc(province);
            if (districts.isEmpty()) {
                // fallback: try English name
                districts = districtRepository.findByProvinceNameInEnglishIgnoreCaseOrderByNameInThaiAsc(province);
            }
        } else {
            districts = districtRepository.findAll();
        }

        return districts.stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "nameInThai", d.getNameInThai() != null ? d.getNameInThai() : "",
                        "nameInEnglish", d.getNameInEnglish() != null ? d.getNameInEnglish() : "",
                        "provinceNameInThai", d.getProvince() != null && d.getProvince().getNameInThai() != null
                                ? d.getProvince().getNameInThai() : ""
                ))
                .collect(Collectors.toList());
    }
}
