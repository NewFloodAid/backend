package com.example.flood_aid.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "admin_districts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"admin_id", "district_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDistrict {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(name = "assigned_at", updatable = false)
    private Timestamp assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @PrePersist
    public void prePersist() {
        this.assignedAt = new Timestamp(System.currentTimeMillis());
    }
}
