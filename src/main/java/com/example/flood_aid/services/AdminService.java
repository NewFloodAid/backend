package com.example.flood_aid.services;

import com.example.flood_aid.models.*;
import com.example.flood_aid.models.dto.auth.CreateAdminRequest;
import com.example.flood_aid.models.dto.auth.UpdateAdminRequest;
import com.example.flood_aid.repositories.AdminDistrictRepository;
import com.example.flood_aid.repositories.AdminRepository;
import com.example.flood_aid.repositories.PasswordResetTokenRepository;
import com.example.flood_aid.repositories.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final AdminDistrictRepository adminDistrictRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public List<Admin> findActiveAdmins() {
        return adminRepository.findByIsActiveTrue();
    }

    public boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void handleSuccessfulLogin(Admin admin) {
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
        adminRepository.save(admin);
    }

    public void handleFailedLogin(Admin admin) {
        int attempts = admin.getFailedLoginAttempts() + 1;
        admin.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            admin.setLockedUntil(new Timestamp(System.currentTimeMillis() + LOCK_DURATION_MS));
            log.warn("Admin account '{}' locked after {} failed attempts", admin.getUsername(), attempts);
        }
        adminRepository.save(admin);
    }

    public List<Long> getDistrictIdsForAdmin(Long adminId) {
        return adminDistrictRepository.findDistrictIdsByAdminId(adminId);
    }

    public List<AdminDistrict> getDistrictsForAdmin(Long adminId) {
        return adminDistrictRepository.findByAdminId(adminId);
    }

    @Transactional
    public Admin createAdmin(CreateAdminRequest request, Long createdByAdminId) {
        if (adminRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (request.getEmail() != null && adminRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Admin admin = Admin.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashPassword(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(AdminRole.fromString(request.getRole()))
                .province(request.getProvince())
                .isActive(true)
                .createdBy(createdByAdminId)
                .build();

        Admin savedAdmin = adminRepository.save(admin);

        // Assign districts if provided
        if (request.getDistrictIds() != null && !request.getDistrictIds().isEmpty()) {
            assignDistricts(savedAdmin.getId(), request.getDistrictIds(), createdByAdminId);
        }

        return savedAdmin;
    }

    @Transactional
    public Admin updateAdmin(Long adminId, UpdateAdminRequest request, Long updatedByAdminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with ID: " + adminId));

        if (request.getEmail() != null) {
            admin.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            admin.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            admin.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            admin.setRole(AdminRole.fromString(request.getRole()));
        }
        if (request.getIsActive() != null) {
            admin.setIsActive(request.getIsActive());
        }
        if (request.getProvince() != null) {
            admin.setProvince(request.getProvince());
        }

        Admin savedAdmin = adminRepository.save(admin);

        // Update district assignments if provided
        if (request.getDistrictIds() != null) {
            adminDistrictRepository.deleteByAdminId(adminId);
            assignDistricts(adminId, request.getDistrictIds(), updatedByAdminId);
        }

        return savedAdmin;
    }

    @Transactional
    public void deactivateAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with ID: " + adminId));
        admin.setIsActive(false);
        adminRepository.save(admin);
    }

    @Transactional
    public void deleteAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found with ID: " + adminId));
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot delete a Super Admin account");
        }
        // Clean up all FK-referenced records first
        passwordResetTokenRepository.deleteByAdminId(adminId);
        refreshTokenRepository.deleteByAdminId(adminId);
        adminDistrictRepository.deleteByAdminId(adminId);
        adminRepository.delete(admin);
    }

    @Transactional
    public void assignDistricts(Long adminId, List<Long> districtIds, Long assignedByAdminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        for (Long districtId : districtIds) {
            District d = new District();
            d.setId(districtId);
            AdminDistrict adminDistrict = AdminDistrict.builder()
                    .admin(admin)
                    .district(d)
                    .assignedBy(assignedByAdminId)
                    .build();
            adminDistrictRepository.save(adminDistrict);
        }
    }
}
