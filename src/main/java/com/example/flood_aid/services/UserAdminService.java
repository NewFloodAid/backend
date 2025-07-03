package com.example.flood_aid.services;

import com.example.flood_aid.models.UserAdmin;
import com.example.flood_aid.repositories.UserAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {

    @Autowired
    private UserAdminRepository userAdminRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAdmin findByUsername(String username) {
        UserAdmin userAdmin = userAdminRepository.findByUsername(username);
        System.out.println(userAdmin == null ? "User not found" : "User found: " + userAdmin.getUsername());
        return userAdmin;
    }
    
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return rawPassword.equals(encodedPassword);
    }

    public void saveUserAdmin(UserAdmin userAdmin) {
        userAdmin.setPassword(passwordEncoder.encode(userAdmin.getPassword()));
        userAdminRepository.save(userAdmin);
    }
    
}