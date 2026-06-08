package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Admin;
import com.example.flood_aid.models.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByEmail(String email);
    List<Admin> findByRole(AdminRole role);
    List<Admin> findByIsActiveTrue();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
