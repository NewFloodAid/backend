package com.example.flood_aid.repositories;

import com.example.flood_aid.models.ImageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ImageCategoryRepository extends JpaRepository<ImageCategory, Long> {
    Optional<ImageCategory> findByName(String name);
}