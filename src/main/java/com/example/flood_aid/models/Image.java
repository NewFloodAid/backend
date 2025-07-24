package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "phase")
    private String phase; // 'BEFORE' or 'AFTER'

    @ManyToOne
    @JoinColumn(name = "image_category_id")
    private ImageCategory imageCategory;

    @ManyToOne
    @JoinColumn(name = "report_id")
    @JsonIgnore
    private Report report;

    @JsonProperty("reportId")
    public Long getReportId() {
        return report != null ? report.getId() : null;
    }

    @Transient
    private String url;
}
