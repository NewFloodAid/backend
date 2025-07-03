package com.example.flood_aid.services;

import com.example.flood_aid.models.Subdistrict;
import com.example.flood_aid.repositories.SubdistrictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubdistrictService {
    @Autowired
    private  SubdistrictRepository subdistrictRepository;

    public List<Subdistrict> getSubdistrictsByProvinceName(String provinceName) {
        return subdistrictRepository.findByDistrictProvinceNameInThai(provinceName);
    }
}
