package com.example.flood_aid.controllers;

import com.example.flood_aid.models.Province;
import com.example.flood_aid.repositories.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceRepository provinceRepository;

    @GetMapping
    public List<Map<String, Object>> getAllProvinces() {
        List<Province> provinces = provinceRepository.findAllByOrderByNameInThaiAsc();
        return provinces.stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "nameInThai", p.getNameInThai() != null ? p.getNameInThai() : "",
                        "nameInEnglish", p.getNameInEnglish() != null ? p.getNameInEnglish() : ""
                ))
                .collect(Collectors.toList());
    }
}
