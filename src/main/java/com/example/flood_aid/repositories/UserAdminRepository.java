package com.example.flood_aid.repositories;

import com.example.flood_aid.models.UserAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAdminRepository extends JpaRepository<UserAdmin, Long> {
    UserAdmin findByUsername(String username);
}
