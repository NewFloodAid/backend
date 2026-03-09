package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "assistance_types")
@Data
@JsonIgnoreProperties(value = {"reportAssistances","reportAssistanceLogs"})
public class AssistanceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "extra_field_label")
    private String extraFieldLabel;

    @Column(name = "extra_field_placeholder")
    private String extraFieldPlaceholder;

    @Column(name = "extra_field_required")
    private Boolean extraFieldRequired = false;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "assistanceType")
    private List<ReportAssistance> reportAssistances;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "assistanceType")
    private List<ReportAssistanceLog> reportAssistanceLogs;

}
