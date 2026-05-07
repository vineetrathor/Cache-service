package com.vineet.cache_project.config;

import com.vineet.cache_project.interceptor.RequestTimingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * This class configures Spring MVC components like interceptors, CORS, etc.
 * 
 * WebMvcConfigurer: Interface to customize Spring MVC configuration
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final RequestTimingInterceptor requestTimingInterceptor;
    
    /**
     * Register interceptors to intercept HTTP requests.
     * The RequestTimingInterceptor will measure execution time for all API requests.
     * 
     * @param registry InterceptorRegistry to add interceptors
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTimingInterceptor)
                .addPathPatterns("/api/**")  // Apply to all /api/* endpoints
                .excludePathPatterns(
                        "/swagger-ui/**",     // Exclude Swagger UI
                        "/v3/api-docs/**",    // Exclude API docs
                        "/api-docs/**"        // Exclude API docs
                );
        
        // You can add more interceptors here if needed
        // registry.addInterceptor(anotherInterceptor).addPathPatterns("/**");
    }
}

// Made with Bob
