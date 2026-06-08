package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Subdistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubdistrictRepository extends JpaRepository<Subdistrict, Long> {
    List<Subdistrict> findByDistrictProvinceNameInThai(String provinceNameInThai);

    @Query(value = "SELECT * FROM subdistricts s " +
            "WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL " +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
            "sin(radians(s.latitude)))) ASC LIMIT 1", nativeQuery = true)
    Optional<Subdistrict> findNearestByCoordinates(@Param("lat") double latitude, @Param("lng") double longitude);
}
