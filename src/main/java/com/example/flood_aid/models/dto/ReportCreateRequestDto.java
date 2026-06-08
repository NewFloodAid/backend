package com.example.flood_aid.models.dto;

import com.example.flood_aid.models.Location;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.ReportAssistance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequestDto {

    private String firstName;
    private String lastName;

    @NotNull(message = "Location is required")
    private Location location;

    @NotBlank(message = "Main phone number is required")
    private String mainPhoneNumber;

    private String reservePhoneNumber;
    private Boolean isAnonymous;
    private String additionalDetail;
    private List<ReportAssistance> reportAssistances;

    public Report toEntity() {
        Report report = new Report();
        report.setFirstName(this.firstName);
        report.setLastName(this.lastName);
        report.setLocation(this.location);
        report.setMainPhoneNumber(this.mainPhoneNumber);
        report.setReservePhoneNumber(this.reservePhoneNumber);
        report.setIsAnonymous(this.isAnonymous != null ? this.isAnonymous : false);
        report.setAdditionalDetail(this.additionalDetail);
        
        if (this.reportAssistances != null) {
            this.reportAssistances.forEach(ra -> ra.setReport(report));
            report.setReportAssistances(this.reportAssistances);
        }
        return report;
    }
}
