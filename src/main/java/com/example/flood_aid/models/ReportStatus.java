package com.example.flood_aid.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

@Entity
@Data
@Table(name = "report_status")
public class ReportStatus {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "user_ordering_number")
    private int userOrderingNumber;

    @Column(name = "government_ordering_number")
    private int governmentOrderingNumber;
}