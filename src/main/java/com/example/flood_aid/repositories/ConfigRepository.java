package com.example.flood_aid.repositories;

import com.example.flood_aid.models.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository   extends JpaRepository<Config, Long> {

    Optional<Config> findByKey(String key);
}
