package com.vineet.cache_project.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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
            
            // Log with different levels based on duration
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
            
            // Log exception if any
            if (ex != null) {
                log.error("❌ Request Failed: {} {} - Exception: {}", 
                        method, uri, ex.getMessage());
            }
        }
    }
}

// Made with Bob
