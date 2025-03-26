package com.example.online_learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Online Learning API", version = "1.0", description = "API documentation for the Online Learning Management System"))
public class OnlineLearningApplication {
	public static void main(String[] args) {
		SpringApplication.run(OnlineLearningApplication.class, args);
	}
}
// Authentication using authentication manager