package com.vineet.cache_project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * This class configures the Swagger UI documentation for our Cache Service API
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI cacheServiceOpenAPI() {
        // Define server information
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8081");
        localServer.setDescription("Local Development Server");
        
        // Define contact information
        Contact contact = new Contact();
        contact.setName("Vineet");
        contact.setEmail("vineet@example.com");
        
        // Define license information
        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
        
        // Define API information
        Info info = new Info()
                .title("Cache Service API")
                .version("1.0.0")
                .description("A simple cache service built with Spring Boot and MySQL. " +
                        "This service provides REST endpoints to store, retrieve, and manage cached data.")
                .contact(contact)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

// Made with Bob
