package com.example.flood_aid.models;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "configs")
@Data
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;

}
