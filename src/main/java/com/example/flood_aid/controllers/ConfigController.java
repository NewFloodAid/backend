package com.example.flood_aid.controllers;


import com.example.flood_aid.models.Config;
import com.example.flood_aid.services.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configs")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping()
    public Config getConfigs(
            @RequestParam() String key
    ) {
        return configService.getConfig(key);
    }

}
