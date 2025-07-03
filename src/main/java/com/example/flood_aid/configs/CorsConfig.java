package com.example.flood_aid.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000" , "http://localhost:3001", "https://onspot-government.kingsglaive.me", "https://onspot-line.kingsglaive.me" , "https://onspot-government.vercel.app", "https://onspot-line.vercel.app/")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD")
                        .allowCredentials(true)
                        .allowedHeaders("*");
            }
        };
    }
}
