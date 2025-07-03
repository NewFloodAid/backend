package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.flood_aid.models.ReportStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportStatusRepository extends JpaRepository<ReportStatus, Long> {
    Optional<ReportStatus> findByStatus(Status status);

    List<ReportStatus> findAllByOrderByUserOrderingNumber();
    List<ReportStatus> findAllByOrderByGovernmentOrderingNumber();
}
