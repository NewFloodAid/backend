package com.example.flood_aid.repositories;

import com.example.flood_aid.models.AssistanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistanceTypeRepository extends JpaRepository<AssistanceType, Long> {
}
