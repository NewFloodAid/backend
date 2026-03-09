package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

        @EntityGraph(attributePaths = { "location", "reportStatus", "reportAssistances",
                        "reportAssistances.assistanceType" })
        @Query("SELECT r FROM Report r " +
                        "WHERE (:userId IS NULL OR r.userId = :userId) AND " +
                        "r.createdAt BETWEEN :startDate AND :endDate")
        List<Report> findReportsByConditions(
                        @Param("userId") UUID userId,
                        @Param("startDate") Timestamp startDate,
                        @Param("endDate") Timestamp endDate);

        @EntityGraph(attributePaths = { "location", "reportStatus", "reportAssistances",
                        "reportAssistances.assistanceType" })
        @Query("SELECT DISTINCT r FROM Report r WHERE r.id IN :ids")
        List<Report> findDetailedReportsByIds(@Param("ids") List<Long> ids);

}
