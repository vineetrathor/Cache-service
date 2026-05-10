package com.vineet.cache_project.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Interceptor to measure and log the execution time of each API request.
 * This intercepts all HTTP requests and calculates the time taken to process them.
 * 
 * How it works:
 * 1. preHandle() - Called before the controller method executes (records start time)
 * 2. Controller method executes
 * 3. postHandle() - Called after controller but before view rendering
 * 4. afterCompletion() - Called after everything is complete (calculates duration)
 */
@Slf4j
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String LOG_FILE_PATH = "cache-project/performance-logs.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Initialize CSV file with headers if it doesn't exist
    public RequestTimingInterceptor() {
        try {
            Path logPath = Paths.get(LOG_FILE_PATH);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath.getParent());
                try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, false))) {
                    writer.println("Timestamp,Method,Endpoint,Key,Status,Duration(ms),Cache Type,Performance");
                }
                log.info("Created performance log file: {}", LOG_FILE_PATH);
            }
        } catch (IOException e) {
            log.error("Failed to initialize performance log file", e);
        }
    }
    
    /**
     * Called before the controller method is executed.
     * We record the start time here.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        log.info("⏱️  Request Started: {} {}", request.getMethod(), request.getRequestURI());
        return true; // Continue with the request
    }
    
    /**
     * Called after the controller method executes but before the view is rendered.
     * We can add response headers here.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        // Add custom header with processing time
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            response.setHeader("X-Response-Time", duration + "ms");
        }
    }
    
    /**
     * Called after the complete request has finished (after view rendering).
     * We calculate and log the total execution time here.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            
            // Extract key from URI if present
            String key = extractKeyFromUri(uri);
            
            // Determine cache type and performance
            String cacheType = determineCacheType(uri, duration);
            String performance = determinePerformance(duration);
            
            // Log exception if any
            if (ex != null) {
                log.error("❌ Request Failed: {} {} - Status: {} - Duration: {}ms - Exception: {}",
                        method, uri, status, duration, ex.getMessage());
                // Don't log failed requests to CSV
                return;
            }
            
            // Only log successful requests (status 2xx or 404 for not found)
            if (status >= 200 && status < 300 || status == 404) {
                // Log to console with different levels based on duration
                if (duration > 1000) {
                    log.warn("⚠️  SLOW REQUEST: {} {} - Status: {} - Duration: {}ms",
                            method, uri, status, duration);
                } else if (duration > 500) {
                    log.info("⏱️  Request Completed: {} {} - Status: {} - Duration: {}ms",
                            method, uri, status, duration);
                } else {
                    log.info("✅ Request Completed: {} {} - Status: {} - Duration: {}ms",
                            method, uri, status, duration);
                }
                
                // Write to CSV file only for successful requests
                logToFile(method, uri, key, status, duration, cacheType, performance);
            } else {
                log.error("❌ Request Failed: {} {} - Status: {} - Duration: {}ms",
                        method, uri, status, duration);
            }
        }
    }
    
    /**
     * Extract key from URI path
     */
    private String extractKeyFromUri(String uri) {
        if (uri.contains("/api/cache/") || uri.contains("/api/redis-cache/")) {
            String[] parts = uri.split("/");
            if (parts.length > 3) {
                return parts[parts.length - 1];
            }
        }
        return "-";
    }
    
    /**
     * Determine cache type based on URI and duration
     */
    private String determineCacheType(String uri, long duration) {
        if (uri.contains("/api/redis-cache/")) {
            return duration < 50 ? "Redis Cache" : "MySQL + Redis";
        } else if (uri.contains("/api/cache/")) {
            return "MySQL Only";
        }
        return "N/A";
    }
    
    /**
     * Determine performance category
     */
    private String determinePerformance(long duration) {
        if (duration < 20) return "Excellent";
        if (duration < 100) return "Good";
        if (duration < 500) return "Fair";
        if (duration < 1000) return "Slow";
        return "Very Slow";
    }
    
    /**
     * Write request log to CSV file
     */
    private void logToFile(String method, String uri, String key, int status,
                          long duration, String cacheType, String performance) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            writer.printf("%s,%s,%s,%s,%d,%d,%s,%s%n",
                    timestamp, method, uri, key, status, duration, cacheType, performance);
        } catch (IOException e) {
            log.error("Failed to write to performance log file", e);
        }
    }
}

// Made with Bob
