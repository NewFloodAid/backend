package com.example.flood_aid.models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.*;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "main_phone_number")
    private String mainPhoneNumber;

    @Column(name = "reserve_phone_number")
    private String reservePhoneNumber;

    @Column(name = "priority")
    private Integer priority;

    @OneToOne()
    @JoinColumn(name = "report_status_id")
    private ReportStatus reportStatus;

    @Column(name = "additional_detail")
    private String additionalDetail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp  createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp  updatedAt;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "report")
    @JsonManagedReference
    private List<ReportAssistance> reportAssistances;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL,mappedBy = "report")
    private List<ReportAssistanceLog> reportAssistanceLogs;


    @OneToMany(cascade = CascadeType.ALL,mappedBy = "report")
    private List<Image> images;

    public void sortReportAssistancesByPriorityDesc() {
        if (this.reportAssistances != null) {
            Collections.sort(this.reportAssistances, new Comparator<ReportAssistance>() {
                @Override
                public int compare(ReportAssistance a1, ReportAssistance a2) {
                    return Integer.compare(a2.getAssistanceType().getPriority(), a1.getAssistanceType().getPriority());
                }
            });
        }
    }
}
