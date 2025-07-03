package com.example.flood_aid.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "provinces")
@Data
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private Integer code;

    @Column(name = "name_in_thai")
    private String nameInThai;

    @Column(name = "name_in_english")
    private String nameInEnglish;

    @OneToMany(cascade = CascadeType.ALL,mappedBy = "province")
    @JsonIgnore
    private List<District> districts;
}
