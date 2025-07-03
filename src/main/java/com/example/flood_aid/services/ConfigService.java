package com.example.flood_aid.services;


import com.example.flood_aid.models.Config;
import com.example.flood_aid.repositories.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    @Autowired
    ConfigRepository configRepository;

    public Config getConfig(String key) {
        return configRepository.findByKey(key).orElseThrow();
    }

}
