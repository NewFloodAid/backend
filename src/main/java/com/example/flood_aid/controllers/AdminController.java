package com.example.flood_aid.controllers;

import com.example.flood_aid.configs.JwtPrincipal;
import com.example.flood_aid.models.Admin;
import com.example.flood_aid.models.AuditLog;
import com.example.flood_aid.models.dto.PaginatedResponse;
import com.example.flood_aid.models.dto.auth.AdminProfileResponse;
import com.example.flood_aid.models.dto.auth.CreateAdminRequest;
import com.example.flood_aid.models.dto.auth.UpdateAdminRequest;
import com.example.flood_aid.services.AdminService;
import com.example.flood_aid.services.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/admins")
    public ResponseEntity<List<AdminProfileResponse>> listAdmins() {
        List<Admin> admins = adminService.findAll();
        List<AdminProfileResponse> response = admins.stream()
                .map(admin -> {
                    var districts = adminService.getDistrictsForAdmin(admin.getId());
                    return AdminProfileResponse.builder()
                            .id(admin.getId())
                            .username(admin.getUsername())
                            .email(admin.getEmail())
                            .fullName(admin.getFullName())
                            .phone(admin.getPhone())
                            .role(admin.getRole())
                            .isActive(admin.getIsActive())
                            .lastLoginAt(admin.getLastLoginAt())
                            .province(admin.getProvince())
                            .assignedDistricts(districts.stream()
                                    .map(ad -> AdminProfileResponse.DistrictInfo.builder()
                                            .id(ad.getDistrict().getId())
                                            .nameInThai(ad.getDistrict().getNameInThai())
                                            .nameInEnglish(ad.getDistrict().getNameInEnglish())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request, HttpServletRequest httpRequest) {
        JwtPrincipal principal = getCurrentJwtPrincipal();
        try {
            Admin admin = adminService.createAdmin(request, principal.adminId());
            auditLogService.log(principal.adminId(), "CREATE", "ADMIN", admin.getId(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok(Map.of("message", "Admin created successfully", "adminId", admin.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody UpdateAdminRequest request,
                                          HttpServletRequest httpRequest) {
        JwtPrincipal principal = getCurrentJwtPrincipal();
        try {
            Admin admin = adminService.updateAdmin(id, request, principal.adminId());
            auditLogService.log(principal.adminId(), "UPDATE", "ADMIN", admin.getId(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok(Map.of("message", "Admin updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deactivateAdmin(@PathVariable Long id, HttpServletRequest httpRequest) {
        JwtPrincipal principal = getCurrentJwtPrincipal();
        try {
            adminService.deactivateAdmin(id);
            auditLogService.log(principal.adminId(), "DEACTIVATE", "ADMIN", id, httpRequest.getRemoteAddr());
            return ResponseEntity.ok(Map.of("message", "Admin deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getAuditLogs(page, size);
        return ResponseEntity.ok(PaginatedResponse.from(logs));
    }

    private JwtPrincipal getCurrentJwtPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        throw new RuntimeException("Not authenticated");
    }
}
