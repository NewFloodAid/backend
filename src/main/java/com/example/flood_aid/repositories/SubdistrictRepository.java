package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Subdistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubdistrictRepository extends JpaRepository<Subdistrict, Long> {
    List<Subdistrict> findByDistrictProvinceNameInThai(String provinceNameInThai);
}

