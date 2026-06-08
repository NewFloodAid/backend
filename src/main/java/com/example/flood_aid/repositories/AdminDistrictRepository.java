package com.example.flood_aid.repositories;

import com.example.flood_aid.models.AdminDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminDistrictRepository extends JpaRepository<AdminDistrict, Long> {
    List<AdminDistrict> findByAdminId(Long adminId);
    List<AdminDistrict> findByDistrictId(Long districtId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AdminDistrict ad WHERE ad.admin.id = :adminId")
    void deleteByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT ad.district.id FROM AdminDistrict ad WHERE ad.admin.id = :adminId")
    List<Long> findDistrictIdsByAdminId(@Param("adminId") Long adminId);
}
