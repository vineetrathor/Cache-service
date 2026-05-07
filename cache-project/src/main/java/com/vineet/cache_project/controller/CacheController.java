package com.vineet.cache_project.controller;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for cache operations.
 * Handles HTTP requests and delegates business logic to CacheService.
 *
 * Base URL: /api/cache
 *
 * @RestController: Combines @Controller and @ResponseBody
 * @RequestMapping: Maps all endpoints to /api/cache
 */
@RestController
@RequestMapping("/api/cache")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Cache Controller", description = "APIs for managing cache entries")
public class CacheController {
    
    private final CacheService cacheService;
    
    /**
     * PUT endpoint: Store or update a cache entry.
     * 
     * URL: PUT /api/cache/{key}
     * Body: { "value": "your_value_here" }
     * 
     * Example:
     * PUT http://localhost:8080/api/cache/user123
     * Body: { "value": "John Doe" }
     * 
     * @param key The cache key (from URL path)
     * @param payload Request body containing the value
     * @return ResponseEntity with the saved cache entry
     */
    @Operation(
            summary = "Store or update a cache entry",
            description = "Creates a new cache entry or updates an existing one with the given key and value"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache entry saved successfully",
                    content = @Content(schema = @Schema(implementation = CacheEntry.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request - value is missing")
    })
    @PutMapping("/{key}")
    public ResponseEntity<CacheEntry> putCache(
            @Parameter(description = "Unique cache key", required = true)
            @PathVariable String key,
            @Parameter(description = "Request body containing the value to cache", required = true)
            @RequestBody Map<String, String> payload) {
        
        log.info("Received PUT request for key: {}", key);
        
        String value = payload.get("value");
        if (value == null) {
            log.error("Value is missing in request body");
            return ResponseEntity.badRequest().build();
        }
        
        CacheEntry savedEntry = cacheService.put(key, value);
        return ResponseEntity.ok(savedEntry);
    }
    
    /**
     * GET endpoint: Retrieve a cache entry by key.
     * 
     * URL: GET /api/cache/{key}
     * 
     * Example:
     * GET http://localhost:8080/api/cache/user123
     * 
     * @param key The cache key to retrieve
     * @return ResponseEntity with the cached value or 404 if not found
     */
    @Operation(
            summary = "Retrieve a cache entry",
            description = "Fetches the cached value for the given key"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache entry found"),
            @ApiResponse(responseCode = "404", description = "Cache entry not found")
    })
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getCache(
            @Parameter(description = "Cache key to retrieve", required = true)
            @PathVariable String key) {
        log.info("Received GET request for key: {}", key);
        
        Optional<String> value = cacheService.get(key);
        
        if (value.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "key", key,
                    "value", value.get()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Key not found: " + key));
        }
    }
    
    /**
     * DELETE endpoint: Remove a cache entry.
     * 
     * URL: DELETE /api/cache/{key}
     * 
     * Example:
     * DELETE http://localhost:8080/api/cache/user123
     * 
     * @param key The cache key to delete
     * @return ResponseEntity with success/failure message
     */
    @Operation(
            summary = "Delete a cache entry",
            description = "Removes the cache entry with the specified key"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache entry deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Cache entry not found")
    })
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, String>> deleteCache(
            @Parameter(description = "Cache key to delete", required = true)
            @PathVariable String key) {
        log.info("Received DELETE request for key: {}", key);
        
        boolean deleted = cacheService.delete(key);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Successfully deleted key: " + key));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Key not found: " + key));
        }
    }
    
    /**
     * GET endpoint: Retrieve all cache entries.
     * 
     * URL: GET /api/cache
     * 
     * Example:
     * GET http://localhost:8080/api/cache
     * 
     * @return ResponseEntity with list of all cache entries
     */
    @Operation(
            summary = "Get all cache entries",
            description = "Retrieves all cache entries stored in the database"
    )
    @ApiResponse(responseCode = "200", description = "List of all cache entries")
    @GetMapping
    public ResponseEntity<List<CacheEntry>> getAllCache() {
        log.info("Received GET request for all cache entries");
        
        List<CacheEntry> entries = cacheService.getAllEntries();
        return ResponseEntity.ok(entries);
    }
    
    /**
     * DELETE endpoint: Clear all cache entries.
     * 
     * URL: DELETE /api/cache
     * 
     * Example:
     * DELETE http://localhost:8080/api/cache
     * 
     * @return ResponseEntity with number of entries deleted
     */
    @Operation(
            summary = "Clear all cache entries",
            description = "Deletes all cache entries from the database"
    )
    @ApiResponse(responseCode = "200", description = "All cache entries cleared successfully")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        log.info("Received DELETE request to clear all cache");
        
        long count = cacheService.clearAll();
        return ResponseEntity.ok(Map.of(
                "message", "Cache cleared successfully",
                "deletedCount", count
        ));
    }
    
    /**
     * Health check endpoint.
     * 
     * URL: GET /api/cache/health
     * 
     * @return Simple health status
     */
    @Operation(
            summary = "Health check",
            description = "Check if the cache service is running"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Cache Service"
        ));
    }
}

// Made with Bob
