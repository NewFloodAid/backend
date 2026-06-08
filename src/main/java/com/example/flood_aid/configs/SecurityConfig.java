package com.example.flood_aid.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(request -> {
            var config = new org.springframework.web.cors.CorsConfiguration();
            config.setAllowedOrigins(java.util.List.of("*"));
            config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(java.util.List.of("*"));
            return config;
        })).csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> {
                // Public endpoints
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api").permitAll() // health check
                    .requestMatchers("/error").permitAll() // allow error page to show actual error

                    // Report endpoints - require authentication
                    .requestMatchers("/api/reports/**").authenticated()

                    // Assistance types - read for all authenticated, write for admins
                    .requestMatchers(HttpMethod.GET, "/api/assistanceTypes").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/assistanceTypes/all").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "WEB_USER")
                    .requestMatchers(HttpMethod.POST, "/api/assistanceTypes").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "WEB_USER")
                    .requestMatchers(HttpMethod.PUT, "/api/assistanceTypes/**").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "WEB_USER")
                    .requestMatchers(HttpMethod.DELETE, "/api/assistanceTypes/**").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "WEB_USER")

                    // Excel export - admin only
                    .requestMatchers("/api/excel/**").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "WEB_USER")

                    // Image deletion - authenticated
                    .requestMatchers(HttpMethod.DELETE, "/api/images/**").authenticated()

                    // Config, report statuses, subdistricts, districts, provinces - authenticated
                    .requestMatchers(HttpMethod.GET, "/api/configs").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/reportStatuses").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/subdistricts").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/districts").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/provinces").authenticated()

                    // Admin management - super admin only
                    .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")

                    // Default - require authentication
                    .anyRequest().authenticated();
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }
}
