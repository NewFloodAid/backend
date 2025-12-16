package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "report_assistances", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "report_id", "assistance_type_id" })
})
@Data
public class ReportAssistance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "report_id")
    @JsonBackReference
    private Report report;

    @JsonProperty("reportId")
    public Long getReportId() {
        return report != null ? report.getId() : null;
    }

    @ManyToOne
    @JoinColumn(name = "assistance_type_id")
    private AssistanceType assistanceType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "is_active")
    private Boolean isActive;
}
