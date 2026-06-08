package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {
    List<Province> findAllByOrderByNameInThaiAsc();
}
