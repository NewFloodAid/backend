package com.example.flood_aid.models.dto.auth;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAdminRequest {
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private Boolean isActive;
    private String province;
    private List<Long> districtIds;
}
