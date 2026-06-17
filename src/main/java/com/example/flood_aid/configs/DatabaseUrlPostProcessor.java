package com.example.flood_aid.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Render's DATABASE_URL (postgres://user:pass@host:port/dbname)
 * into Spring Boot's required format (jdbc:postgresql://host:port/dbname)
 * and extracts username/password into separate properties.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("spring.datasource.url");

        System.out.println("DatabaseUrlPostProcessor: Raw datasource URL = " + (dbUrl != null ? dbUrl.substring(0, Math.min(dbUrl.length(), 30)) + "..." : "null"));

        if (dbUrl == null || dbUrl.isEmpty()) {
            System.out.println("DatabaseUrlPostProcessor: No datasource URL found!");
            return;
        }

        if (!dbUrl.startsWith("postgres://") && !dbUrl.startsWith("postgresql://")) {
            System.out.println("DatabaseUrlPostProcessor: URL already in JDBC format, no conversion needed.");
            return;
        }

        try {
            // Parse: postgres://user:password@host:port/dbname
            URI uri = new URI(dbUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath(); // /dbname
            String userInfo = uri.getUserInfo(); // user:password

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", jdbcUrl);

            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                map.put("spring.datasource.username", parts[0]);
                map.put("spring.datasource.password", parts[1]);
                System.out.println("DatabaseUrlPostProcessor: Extracted username = " + parts[0]);
            }

            environment.getPropertySources().addFirst(new MapPropertySource("fixedDatabaseUrl", map));
            System.out.println("DatabaseUrlPostProcessor: Converted to " + jdbcUrl);
        } catch (Exception e) {
            System.out.println("DatabaseUrlPostProcessor: Failed to parse URL: " + e.getMessage());
        }
    }
}
