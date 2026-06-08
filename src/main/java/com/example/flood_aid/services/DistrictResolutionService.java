package com.example.flood_aid.services;

import com.example.flood_aid.models.District;
import com.example.flood_aid.models.Subdistrict;
import com.example.flood_aid.repositories.SubdistrictRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class DistrictResolutionService {

    private final SubdistrictRepository subdistrictRepository;

    /**
     * Resolves the district for a given coordinate by finding the nearest subdistrict.
     * Uses the Haversine formula to calculate distance.
     */
    public District resolveDistrict(double latitude, double longitude) {
        Optional<Subdistrict> nearest = subdistrictRepository.findNearestByCoordinates(latitude, longitude);
        if (nearest.isEmpty()) {
            log.warn("Could not resolve district for coordinates ({}, {})", latitude, longitude);
            return null;
        }
        Subdistrict subdistrict = nearest.get();
        log.info("Resolved coordinates ({}, {}) to district: {} via subdistrict: {}",
                latitude, longitude,
                subdistrict.getDistrict().getNameInThai(),
                subdistrict.getNameInThai());
        return subdistrict.getDistrict();
    }
}
