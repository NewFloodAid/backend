package com.example.flood_aid.repositories;
import com.example.flood_aid.models.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r " +
            "WHERE (:priorities IS NOT NULL AND (r.priority IN :priorities ) OR r.priority = 0  OR r.priority = 4) AND " +
            "(:userId IS NULL OR r.userId = :userId) AND " +
            "r.createdAt BETWEEN :startDate AND :endDate AND" +
            "((:isContainRejectedReport = true AND r.reportStatus.id IN (1,2,3,4)) OR " +
            " (:isContainRejectedReport = false AND r.reportStatus.id IN (2,3,4)))")
    List<Report> findReportsByConditions(
            @Param("userId") UUID userId,
            @Param("priorities") ArrayList<Integer> priorities,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            @Param("isContainRejectedReport") boolean isContainRejectedReport
    );

    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.location l " +
            "WHERE r IN :reports AND " +
            "(:subdistrict IS NULL OR l.subDistrict = :subdistrict) AND " +
            "(:district IS NULL OR l.district = :district) AND " +
            "(:province IS NULL OR l.province = :province) AND " +
            "(:postalCode IS NULL OR l.postalCode = :postalCode)")
    List<Report> filterReportsByLocation(
            @Param("reports") List<Report> reports,
            @Param("subdistrict") String subdistrict,
            @Param("district") String district,
            @Param("province") String province,
            @Param("postalCode") String postalCode
    );


    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.reportStatus rs " +
            "WHERE r IN :reports AND " +
            "(:reportStatusId IS NULL OR rs.id = :reportStatusId) " +
            "ORDER BY CASE WHEN :sourceApp = 'LIFF' THEN rs.userOrderingNumber ELSE rs.governmentOrderingNumber END")
    List<Report> filterReportsByStatus(
            @Param("reports") List<Report> reports,
            @Param("reportStatusId") Long reportStatusId,
            @Param("sourceApp") String sourceApp
    );

}
