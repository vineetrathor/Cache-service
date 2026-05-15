package com.vineet.cache_project.controller;

import com.vineet.cache_project.service.WriteThroughCacheService;
import com.vineet.cache_project.service.CacheAsideService;
import com.vineet.cache_project.service.WriteAroundService;
import com.vineet.cache_project.service.WriteBackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified REST Controller for all 4 Cache Patterns
 * 
 * This controller exposes endpoints for:
 * 1. Write-Through: Immediate caching on write, fast first reads
 * 2. Cache-Aside: Lazy loading, memory efficient
 * 3. Write-Around: Selective caching, prevents cache pollution
 * 4. Write-Back: Async writes, highest throughput
 * 
 * Each pattern has its own set of PUT/GET/DELETE endpoints
 * Plus a comparison endpoint to test all patterns with the same key
 */
@RestController
@RequestMapping("/api/cache-patterns")
@Tag(name = "Cache Patterns", description = "Endpoints for all 4 cache pattern implementations")
public class CachePatternController {

    @Autowired
    private WriteThroughCacheService writeThroughService;

    @Autowired
    private CacheAsideService cacheAsideService;

    @Autowired
    private WriteAroundService writeAroundService;

    @Autowired
    private WriteBackService writeBackService;

    // ==================== WRITE-THROUGH PATTERN ====================

    @Operation(
        summary = "Write-Through: Store data",
        description = "Stores data in both MySQL and Redis immediately. Best for read-heavy workloads with fast first reads."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data stored successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/write-through/{key}")
    public ResponseEntity<Map<String, String>> writeThroughPut(
            @Parameter(description = "Cache key") @PathVariable String key,
            @Parameter(description = "Value to store") @RequestBody String value) {
        
        long startTime = System.currentTimeMillis();
        String result = writeThroughService.put(key, value);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Through");
        response.put("operation", "PUT");
        response.put("key", key);
        response.put("value", result);
        response.put("duration_ms", String.valueOf(duration));
        response.put("description", "Data written to MySQL + Redis immediately");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Through: Retrieve data",
        description = "Retrieves data from Redis cache first, falls back to MySQL if not found. First read is fast."
    )
    @GetMapping("/write-through/{key}")
    public ResponseEntity<Map<String, String>> writeThroughGet(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        long startTime = System.currentTimeMillis();
        String result = writeThroughService.get(key);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Through");
        response.put("operation", "GET");
        response.put("key", key);
        response.put("value", result != null ? result : "null");
        response.put("duration_ms", String.valueOf(duration));
        response.put("cache_hit", result != null ? "likely" : "miss");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Through: Delete data",
        description = "Deletes data from both MySQL and Redis cache"
    )
    @DeleteMapping("/write-through/{key}")
    public ResponseEntity<Map<String, String>> writeThroughDelete(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        writeThroughService.delete(key);

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Through");
        response.put("operation", "DELETE");
        response.put("key", key);
        response.put("status", "deleted from MySQL + Redis");

        return ResponseEntity.ok(response);
    }

    // ==================== CACHE-ASIDE PATTERN ====================

    @Operation(
        summary = "Cache-Aside: Store data",
        description = "Stores data in MySQL only (NO caching on write). Cache is populated lazily on first read. Memory efficient."
    )
    @PutMapping("/cache-aside/{key}")
    public ResponseEntity<Map<String, String>> cacheAsidePut(
            @Parameter(description = "Cache key") @PathVariable String key,
            @Parameter(description = "Value to store") @RequestBody String value) {
        
        long startTime = System.currentTimeMillis();
        String result = cacheAsideService.put(key, value);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Cache-Aside");
        response.put("operation", "PUT");
        response.put("key", key);
        response.put("value", result);
        response.put("duration_ms", String.valueOf(duration));
        response.put("description", "Data written to MySQL only, cache NOT populated");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Cache-Aside: Retrieve data",
        description = "Lazy loading: checks Redis first, if miss loads from MySQL and caches it. First read is slow, subsequent reads are fast."
    )
    @GetMapping("/cache-aside/{key}")
    public ResponseEntity<Map<String, String>> cacheAsideGet(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        long startTime = System.currentTimeMillis();
        String result = cacheAsideService.get(key);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Cache-Aside");
        response.put("operation", "GET");
        response.put("key", key);
        response.put("value", result != null ? result : "null");
        response.put("duration_ms", String.valueOf(duration));
        response.put("cache_behavior", duration > 20 ? "cache miss (loaded from MySQL)" : "cache hit");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Cache-Aside: Delete data",
        description = "Deletes data from both MySQL and Redis cache"
    )
    @DeleteMapping("/cache-aside/{key}")
    public ResponseEntity<Map<String, String>> cacheAsideDelete(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        cacheAsideService.delete(key);

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Cache-Aside");
        response.put("operation", "DELETE");
        response.put("key", key);
        response.put("status", "deleted from MySQL + Redis");

        return ResponseEntity.ok(response);
    }

    // ==================== WRITE-AROUND PATTERN ====================

    @Operation(
        summary = "Write-Around: Store data",
        description = "Stores data in MySQL only, bypasses cache completely. Prevents cache pollution from write-heavy data."
    )
    @PutMapping("/write-around/{key}")
    public ResponseEntity<Map<String, String>> writeAroundPut(
            @Parameter(description = "Cache key") @PathVariable String key,
            @Parameter(description = "Value to store") @RequestBody String value) {
        
        long startTime = System.currentTimeMillis();
        String result = writeAroundService.put(key, value);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Around");
        response.put("operation", "PUT");
        response.put("key", key);
        response.put("value", result);
        response.put("duration_ms", String.valueOf(duration));
        response.put("description", "Data written to MySQL, cache bypassed");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Around: Retrieve data",
        description = "Selective caching: only caches data after 2+ accesses. Prevents caching of rarely-read data."
    )
    @GetMapping("/write-around/{key}")
    public ResponseEntity<Map<String, String>> writeAroundGet(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        long startTime = System.currentTimeMillis();
        String result = writeAroundService.get(key);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Around");
        response.put("operation", "GET");
        response.put("key", key);
        response.put("value", result != null ? result : "null");
        response.put("duration_ms", String.valueOf(duration));
        response.put("cache_behavior", "selective (caches after 2+ accesses)");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Around: Delete data",
        description = "Deletes data from MySQL and clears any cached data and access counters"
    )
    @DeleteMapping("/write-around/{key}")
    public ResponseEntity<Map<String, String>> writeAroundDelete(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        writeAroundService.delete(key);

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Around");
        response.put("operation", "DELETE");
        response.put("key", key);
        response.put("status", "deleted from MySQL + Redis + access counters");

        return ResponseEntity.ok(response);
    }

    // ==================== WRITE-BACK PATTERN ====================

    @Operation(
        summary = "Write-Back: Store data",
        description = "Fastest writes: stores in Redis immediately, queues for async batch write to MySQL (every 30s). Highest throughput."
    )
    @PutMapping("/write-back/{key}")
    public ResponseEntity<Map<String, String>> writeBackPut(
            @Parameter(description = "Cache key") @PathVariable String key,
            @Parameter(description = "Value to store") @RequestBody String value) {
        
        long startTime = System.currentTimeMillis();
        String result = writeBackService.put(key, value);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Back");
        response.put("operation", "PUT");
        response.put("key", key);
        response.put("value", result);
        response.put("duration_ms", String.valueOf(duration));
        response.put("description", "Data written to Redis immediately, queued for MySQL (batch write every 30s)");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Back: Retrieve data",
        description = "Retrieves data from Redis cache first, falls back to MySQL if not found. Very fast reads."
    )
    @GetMapping("/write-back/{key}")
    public ResponseEntity<Map<String, String>> writeBackGet(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        long startTime = System.currentTimeMillis();
        String result = writeBackService.get(key);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Back");
        response.put("operation", "GET");
        response.put("key", key);
        response.put("value", result != null ? result : "null");
        response.put("duration_ms", String.valueOf(duration));
        response.put("cache_hit", result != null ? "likely" : "miss");

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Write-Back: Delete data",
        description = "Deletes data from both MySQL and Redis cache, removes from write queue"
    )
    @DeleteMapping("/write-back/{key}")
    public ResponseEntity<Map<String, String>> writeBackDelete(
            @Parameter(description = "Cache key") @PathVariable String key) {
        
        writeBackService.delete(key);

        Map<String, String> response = new HashMap<>();
        response.put("pattern", "Write-Back");
        response.put("operation", "DELETE");
        response.put("key", key);
        response.put("status", "deleted from MySQL + Redis + write queue");

        return ResponseEntity.ok(response);
    }

    // ==================== COMPARISON ENDPOINT ====================

    @Operation(
        summary = "Compare all patterns",
        description = "Tests the same key across all 4 cache patterns and returns performance comparison. Use after storing data in each pattern."
    )
    @GetMapping("/compare/{key}")
    public ResponseEntity<Map<String, Object>> comparePatterns(
            @Parameter(description = "Cache key to test across all patterns") @PathVariable String key) {
        
        Map<String, Object> comparison = new HashMap<>();
        
        // Test Write-Through
        long startTime = System.currentTimeMillis();
        String wtResult = writeThroughService.get(key);
        long wtDuration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> wtData = new HashMap<>();
        wtData.put("value", wtResult != null ? wtResult : "null");
        wtData.put("duration_ms", wtDuration);
        wtData.put("characteristics", "Fast first read, immediate caching on write");
        comparison.put("write_through", wtData);
        
        // Test Cache-Aside
        startTime = System.currentTimeMillis();
        String caResult = cacheAsideService.get(key);
        long caDuration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> caData = new HashMap<>();
        caData.put("value", caResult != null ? caResult : "null");
        caData.put("duration_ms", caDuration);
        caData.put("characteristics", "Lazy loading, memory efficient");
        comparison.put("cache_aside", caData);
        
        // Test Write-Around
        startTime = System.currentTimeMillis();
        String waResult = writeAroundService.get(key);
        long waDuration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> waData = new HashMap<>();
        waData.put("value", waResult != null ? waResult : "null");
        waData.put("duration_ms", waDuration);
        waData.put("characteristics", "Selective caching, prevents pollution");
        comparison.put("write_around", waData);
        
        // Test Write-Back
        startTime = System.currentTimeMillis();
        String wbResult = writeBackService.get(key);
        long wbDuration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> wbData = new HashMap<>();
        wbData.put("value", wbResult != null ? wbResult : "null");
        wbData.put("duration_ms", wbDuration);
        wbData.put("characteristics", "Fastest writes, async batch to MySQL");
        comparison.put("write_back", wbData);
        
        // Add summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("fastest_read", wtDuration <= caDuration && wtDuration <= waDuration && wtDuration <= wbDuration ? "write_through" : 
                                    caDuration <= wtDuration && caDuration <= waDuration && caDuration <= wbDuration ? "cache_aside" :
                                    waDuration <= wtDuration && waDuration <= caDuration && waDuration <= wbDuration ? "write_around" : "write_back");
        summary.put("note", "Performance varies based on cache state. Run multiple times for accurate comparison.");
        comparison.put("summary", summary);
        
        return ResponseEntity.ok(comparison);
    }

    // ==================== HEALTH CHECK ====================

    @Operation(
        summary = "Health check",
        description = "Verifies all cache pattern services are available"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("write_through", "available");
        health.put("cache_aside", "available");
        health.put("write_around", "available");
        health.put("write_back", "available");
        health.put("message", "All 4 cache patterns are operational");
        
        return ResponseEntity.ok(health);
    }
}

// Made with Bob
