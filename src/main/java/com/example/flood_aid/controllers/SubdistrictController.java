package com.example.flood_aid.controllers;


import com.example.flood_aid.models.Subdistrict;
import com.example.flood_aid.services.SubdistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subdistricts")
public class SubdistrictController {

    @Autowired
    private  SubdistrictService subdistrictService;

    @GetMapping
    public List<Subdistrict> getSubdistrictByProvinceName(@RequestParam("province") String provinceName) {
        return subdistrictService.getSubdistrictsByProvinceName(provinceName);
    }
}
