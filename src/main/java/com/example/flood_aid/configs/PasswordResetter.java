package com.example.flood_aid.configs;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import com.example.flood_aid.repositories.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.flood_aid.models.Admin;
import java.util.Optional;

@Component
public class PasswordResetter {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetter(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void reset() {
        Optional<Admin> adminOpt = adminRepository.findByUsername("superadmin");
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            admin.setPasswordHash(passwordEncoder.encode("Admin@1234"));
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
            adminRepository.save(admin);
            System.out.println("========== SUPERADMIN PASSWORD RESET TO Admin@1234 ==========");
        }
    }
}
