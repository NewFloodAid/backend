package com.example.flood_aid.repositories;

import com.example.flood_aid.models.AssistanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssistanceTypeRepository extends JpaRepository<AssistanceType, Long> {
    List<AssistanceType> findByIsActiveTrueOrderByIdAsc();

    List<AssistanceType> findAllByOrderByIdAsc();
}
