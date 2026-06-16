package com.example.flood_aid.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && dbUrl.startsWith("postgres://")) {
            String jdbcUrl = dbUrl.replaceFirst("postgres://", "jdbc:postgresql://");
            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", jdbcUrl);
            environment.getPropertySources().addFirst(new MapPropertySource("fixedDatabaseUrl", map));
            System.out.println("🔧 DatabaseUrlPostProcessor: Converted postgres:// to jdbc:postgresql://");
        }
    }
}
