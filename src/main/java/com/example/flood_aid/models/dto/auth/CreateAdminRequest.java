package com.example.flood_aid.models.dto.auth;

import lombok.Data;
import java.util.List;

@Data
public class CreateAdminRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String role; // SUPER_ADMIN or DISTRICT_ADMIN
    private String province; // Province this admin manages (required for SUPER_ADMIN)
    private List<Long> districtIds;
}
