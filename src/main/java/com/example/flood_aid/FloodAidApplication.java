package com.example.flood_aid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class FloodAidApplication {
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(FloodAidApplication.class);
		app.setAdditionalProfiles("dev");
		app.run(args);
	}
}
