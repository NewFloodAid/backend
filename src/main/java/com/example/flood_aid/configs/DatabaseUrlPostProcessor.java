package com.example.flood_aid.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads DATABASE_URL from environment (as provided by Render),
 * converts it from postgres://user:pass@host:port/dbname
 * to jdbc:postgresql://host:port/dbname, and sets Spring datasource properties.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Try DATABASE_URL from environment first
        String databaseUrl = System.getenv("DATABASE_URL");
        System.out.println("DatabaseUrlPostProcessor: DATABASE_URL env = " +
                (databaseUrl != null ? databaseUrl.substring(0, Math.min(databaseUrl.length(), 40)) + "..." : "NOT SET"));

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            // Fall back to spring.datasource.url property
            databaseUrl = environment.getProperty("spring.datasource.url");
            System.out.println("DatabaseUrlPostProcessor: Falling back to spring.datasource.url = " +
                    (databaseUrl != null ? databaseUrl.substring(0, Math.min(databaseUrl.length(), 40)) + "..." : "NOT SET"));
        }

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            System.out.println("DatabaseUrlPostProcessor: No database URL found anywhere!");
            return;
        }

        // If already in JDBC format, nothing to do
        if (databaseUrl.startsWith("jdbc:")) {
            System.out.println("DatabaseUrlPostProcessor: URL already in JDBC format.");
            return;
        }

        // Convert postgres:// or postgresql:// to jdbc:postgresql://
        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                // Normalize to postgresql:// for URI parsing
                String normalized = databaseUrl.startsWith("postgres://")
                        ? "postgresql://" + databaseUrl.substring("postgres://".length())
                        : databaseUrl;

                URI uri = new URI(normalized);
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

                // addFirst ensures these override any other property sources
                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", map));
                System.out.println("DatabaseUrlPostProcessor: Successfully set datasource URL = " + jdbcUrl);
            } catch (Exception e) {
                System.out.println("DatabaseUrlPostProcessor: Failed to parse URL: " + e.getMessage());
            }
        }
    }
}
