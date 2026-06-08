package com.example.flood_aid.models.dto.auth;

import com.example.flood_aid.models.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private AdminRole role;
    private Boolean isActive;
    private Timestamp lastLoginAt;
    private String province;
    private List<DistrictInfo> assignedDistricts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictInfo {
        private Long id;
        private String nameInThai;
        private String nameInEnglish;
    }
}
