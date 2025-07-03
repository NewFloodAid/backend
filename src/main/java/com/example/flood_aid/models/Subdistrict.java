package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "subdistricts")
@Data
public class Subdistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private Integer code;

    @Column(name = "name_in_thai")
    private String nameInThai;

    @Column(name = "name_in_english")
    private String nameInEnglish;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "zip_code")
    private Integer zipCode;

    @ManyToOne
    @JoinColumn(name = "district_id")
    @JsonIgnore
    private District district;

}
