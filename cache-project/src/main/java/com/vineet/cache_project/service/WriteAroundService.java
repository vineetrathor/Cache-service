package com.vineet.cache_project.service;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * WRITE-AROUND PATTERN
 * 
 * Pattern: Write-Around (Bypass Cache on Write)
 * 
 * CHARACTERISTICS:
 * - Writes go ONLY to database (bypass cache completely)
 * - Reads check cache, but DON'T automatically cache on miss
 * - Cache populated manually based on access frequency
 * - Prevents cache pollution from write-heavy data
 * - Most conservative caching strategy
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────┐
 * │  Client Request                                      │
 * └────────────────┬────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 1: Redis Cache (In-Memory) - FAST (~5ms)     │
 * │  - Populated ONLY for frequently accessed data      │
 * │  - Manual cache management                          │
 * │  - Prevents cache pollution                         │
 * └────────────────┬────────────────────────────────────┘
 *                  │ Cache MISS
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 2: MySQL Database - PERSISTENT (~50ms)       │
 * │  - All writes go here                               │
 * │  - Source of truth                                  │
 * └─────────────────────────────────────────────────────┘
 * 
 * FLOW:
 * PUT: MySQL (save) → Return (cache bypassed)
 * GET: Redis (check) → MISS → MySQL (query) → Return (NO auto-cache)
 * GET (frequent): Track access → Cache if accessed multiple times
 * DELETE: MySQL (delete) → Redis (evict if present) → Return
 * 
 * USE CASES:
 * - Write-heavy workloads (logs, events, metrics)
 * - Data rarely read after writing
 * - Preventing cache pollution
 * - When most data is "write-once, read-never"
 * 
 * PROS:
 * ✅ Prevents cache pollution
 * ✅ Optimal memory usage
 * ✅ Good for write-heavy workloads
 * ✅ Cache only truly hot data
 * 
 * CONS:
 * ❌ All reads are slow initially
 * ❌ Requires manual cache management
 * ❌ More complex implementation
 * ❌ Need to track access patterns
 * 
 * CRITICAL: Returns String type to avoid serialization issues!
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WriteAroundService {
    
    private final CacheRepository cacheRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Track access count for each key to decide when to cache
    private static final String ACCESS_COUNT_PREFIX = "access:count:";
    private static final int CACHE_THRESHOLD = 2; // Cache after 2 accesses
    
    /**
     * PUT operation WITHOUT caching (Write-Around pattern)
     * 
     * NO caching at all - writes bypass cache completely
     * 
     * Flow:
     * 1. Save to MySQL (persistent storage)
     * 2. Return value (Redis completely bypassed)
     * 3. Cache will ONLY be populated if data is read frequently
     * 
     * Why bypass cache on write?
     * - Prevents cache pollution from write-heavy data
     * - Most written data is never read
     * - Saves memory for truly hot data
     * - Optimal for write-heavy, read-rarely scenarios
     * 
     * @param key Cache key
     * @param value Value to store
     * @return String value (for consistent type with GET)
     */
    @Transactional
    public String put(String key, String value) {
        log.info("🔵 WRITE-AROUND PUT - Key: {} | Saving to MySQL, bypassing cache", key);
        
        Optional<CacheEntry> existingEntry = cacheRepository.findByKey(key);
        
        if (existingEntry.isPresent()) {
            CacheEntry entry = existingEntry.get();
            entry.setValue(value);
            log.info("📝 Updating existing entry in MySQL for key: {}", key);
            cacheRepository.save(entry);
        } else {
            CacheEntry newEntry = CacheEntry.builder()
                    .key(key)
                    .value(value)
                    .build();
            log.info("✨ Creating new entry in MySQL for key: {}", key);
            cacheRepository.save(newEntry);
        }
        
        // Reset access count since data was updated
        String accessKey = ACCESS_COUNT_PREFIX + key;
        redisTemplate.delete(accessKey);
        
        log.info("✅ Saved to MySQL. Cache bypassed (will cache only if read frequently)");
        return value; // Return String type (consistent with GET)
    }
    
    /**
     * GET operation with selective caching (Write-Around pattern)
     * 
     * NO automatic caching - manual cache management based on access frequency
     * 
     * Flow:
     * 1. Check Redis cache
     * 2. If FOUND (Cache HIT) → Return immediately ⚡ FAST! (~5ms)
     * 3. If NOT FOUND (Cache MISS):
     *    a. Query MySQL (~50ms)
     *    b. Increment access counter
     *    c. If accessed >= threshold → Cache in Redis
     *    d. Return value
     * 
     * Access Tracking:
     * - First access: Query MySQL, don't cache (might never be read again)
     * - Second access: Query MySQL, NOW cache (data is hot!)
     * - Third+ access: Serve from Redis cache ⚡
     * 
     * Why selective caching?
     * - Prevents caching data that's accessed only once
     * - Only caches truly hot data
     * - Optimal memory usage
     * 
     * CRITICAL: Returns String (not Optional<String>) to avoid serialization issues!
     * 
     * @param key Cache key to retrieve
     * @return String value if found, null if not found
     */
    public String get(String key) {
        log.info("🔍 WRITE-AROUND GET - Key: {} | Checking Redis first...", key);
        
        // Check Redis cache first
        String cacheKey = "writeAround::" + key;
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedValue != null) {
            log.info("✅ Redis HIT for key: {} | Returning cached value", key);
            return cachedValue.toString();
        }
        
        // Cache MISS - query MySQL
        log.info("❌ Redis MISS for key: {} | Querying MySQL...", key);
        Optional<CacheEntry> entry = cacheRepository.findByKey(key);
        
        if (entry.isEmpty()) {
            log.info("❌ MySQL MISS for key: {} | Not found anywhere", key);
            return null;
        }
        
        String value = entry.get().getValue();
        log.info("✅ MySQL HIT for key: {}", key);
        
        // Track access count
        String accessKey = ACCESS_COUNT_PREFIX + key;
        Long accessCount = redisTemplate.opsForValue().increment(accessKey);
        redisTemplate.expire(accessKey, 10, TimeUnit.MINUTES);
        
        log.info("📊 Access count for key {}: {}", key, accessCount);
        
        // Cache only if accessed multiple times (hot data)
        if (accessCount != null && accessCount >= CACHE_THRESHOLD) {
            log.info("🔥 HOT DATA detected! Caching key: {} in Redis (accessed {} times)", key, accessCount);
            redisTemplate.opsForValue().set(cacheKey, value, 10, TimeUnit.MINUTES);
        } else {
            log.info("❄️ COLD DATA - Not caching yet (accessed {} times, threshold: {})", 
                    accessCount, CACHE_THRESHOLD);
        }
        
        return value;
    }
    
    /**
     * DELETE operation with cache eviction
     * 
     * @CacheEvict: Removes from Redis if present
     * 
     * Flow:
     * 1. Delete from MySQL
     * 2. Evict from Redis cache (if present)
     * 3. Clear access counter
     * 4. Return success status
     * 
     * @param key Cache key to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    @CacheEvict(value = "writeAround", key = "#key")
    public boolean delete(String key) {
        log.info("🗑️ WRITE-AROUND DELETE - Key: {} | Removing from MySQL and Redis", key);
        
        if (cacheRepository.existsByKey(key)) {
            cacheRepository.deleteByKey(key);
            
            // Clean up access counter
            String accessKey = ACCESS_COUNT_PREFIX + key;
            redisTemplate.delete(accessKey);
            
            // Clean up cache entry
            String cacheKey = "writeAround::" + key;
            redisTemplate.delete(cacheKey);
            
            log.info("✅ Successfully deleted from MySQL and evicted from Redis: {}", key);
            return true;
        } else {
            log.info("❌ Key not found: {}", key);
            return false;
        }
    }
    
    /**
     * Get all cache entries (not cached - for monitoring)
     * 
     * @return List of all entries
     */
    public List<CacheEntry> getAllEntries() {
        log.info("📋 Fetching all cache entries from MySQL");
        return cacheRepository.findAll();
    }
    
    /**
     * Clear all cache entries
     * 
     * Clears MySQL, Redis cache, and access counters
     * 
     * @return Number of entries deleted
     */
    @Transactional
    public long clearAll() {
        log.info("🧹 Clearing all entries from MySQL and Redis");
        long count = cacheRepository.count();
        cacheRepository.deleteAll();
        
        // Clear all access counters
        redisTemplate.delete(redisTemplate.keys(ACCESS_COUNT_PREFIX + "*"));
        
        // Clear all cache entries
        redisTemplate.delete(redisTemplate.keys("writeAround::*"));
        
        log.info("✅ Cleared {} entries from MySQL and Redis", count);
        return count;
    }
}

// Made with Bob