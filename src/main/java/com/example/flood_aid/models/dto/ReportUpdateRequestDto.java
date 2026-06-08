package com.example.flood_aid.models.dto;

import com.example.flood_aid.models.Location;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.ReportAssistance;
import com.example.flood_aid.models.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReportUpdateRequestDto {

    @NotNull(message = "Report ID is required for update")
    private Long id;
    
    private String firstName;
    private String lastName;
    private Location location;
    private String mainPhoneNumber;
    private String reservePhoneNumber;
    private Boolean isAnonymous;
    private String additionalDetail;
    private String afterAdditionalDetail;
    private ReportStatus reportStatus;
    private List<ReportAssistance> reportAssistances;

    public Report toEntity() {
        Report report = new Report();
        report.setId(this.id);
        report.setFirstName(this.firstName);
        report.setLastName(this.lastName);
        report.setLocation(this.location);
        report.setMainPhoneNumber(this.mainPhoneNumber);
        report.setReservePhoneNumber(this.reservePhoneNumber);
        report.setIsAnonymous(this.isAnonymous != null ? this.isAnonymous : false);
        report.setAdditionalDetail(this.additionalDetail);
        report.setAfterAdditionalDetail(this.afterAdditionalDetail);
        report.setReportStatus(this.reportStatus);
        
        if (this.reportAssistances != null) {
            this.reportAssistances.forEach(ra -> ra.setReport(report));
            report.setReportAssistances(this.reportAssistances);
        }
        return report;
    }
}
