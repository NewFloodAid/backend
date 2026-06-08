package com.example.flood_aid.repositories;

import com.example.flood_aid.models.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByProvinceNameInThaiOrderByNameInThaiAsc(String provinceNameInThai);
    List<District> findByProvinceNameInEnglishIgnoreCaseOrderByNameInThaiAsc(String provinceNameInEnglish);
}
