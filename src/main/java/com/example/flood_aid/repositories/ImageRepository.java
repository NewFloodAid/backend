package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    @Query("SELECT i FROM Image i WHERE i.report.id IN :reportIds")
    List<Image> findAllByReportIds(@Param("reportIds") List<Long> reportIds);
}
