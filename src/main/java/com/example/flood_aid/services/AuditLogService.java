package com.example.flood_aid.services;

import com.example.flood_aid.models.AuditLog;
import com.example.flood_aid.repositories.AuditLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@AllArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long adminId, String action, String entityType, Long entityId,
                    String oldValue, String newValue, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .adminId(adminId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(auditLog);
    }

    public void log(Long adminId, String action, String entityType, Long entityId, String ipAddress) {
        log(adminId, action, entityType, entityId, null, null, ipAddress, null);
    }

    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
    }

    public Page<AuditLog> getAuditLogsByAdmin(Long adminId, int page, int size) {
        return auditLogRepository.findByAdminId(adminId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public Page<AuditLog> getAuditLogsByDateRange(Timestamp startDate, Timestamp endDate, int page, int size) {
        return auditLogRepository.findByCreatedAtBetween(startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
