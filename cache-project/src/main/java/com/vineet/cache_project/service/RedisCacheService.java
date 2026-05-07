package com.vineet.cache_project.service;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Redis-Enabled Cache Service with 2-TIER CACHING SYSTEM
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────┐
 * │  Client Request                                      │
 * └────────────────┬────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 1: Redis Cache (In-Memory) - FAST (~5ms)     │
 * │  - Stores frequently accessed data                  │
 * │  - TTL: 10 minutes                                  │
 * │  - Volatile (lost on restart)                       │
 * └────────────────┬────────────────────────────────────┘
 *                  │ Cache MISS
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 2: MySQL Database - PERSISTENT (~50ms)       │
 * │  - Permanent storage                                │
 * │  - Slower but reliable                              │
 * └─────────────────────────────────────────────────────┘
 * 
 * FLOW:
 * GET: Redis → (if miss) → MySQL → Store in Redis → Return
 * PUT: MySQL → Update Redis → Return
 * DELETE: MySQL → Evict from Redis → Return
 * 
 * Spring Cache Annotations:
 * @Cacheable: Check cache before method execution
 * @CachePut: Update cache after method execution
 * @CacheEvict: Remove from cache
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService {
    
    private final CacheRepository cacheRepository;
    
    /**
     * PUT operation with Redis caching
     * 
     * @CachePut: Always executes method AND updates Redis
     * 
     * Flow:
     * 1. Save/Update in MySQL (persistent)
     * 2. Update Redis cache (fast access)
     * 3. Return saved entry
     * 
     * @param key Cache key
     * @param value Value to store
     * @return Saved CacheEntry
     */
    @Transactional
    @CachePut(value = "cacheEntries", key = "#key")
    public CacheEntry put(String key, String value) {
        log.info("🔵 REDIS PUT - Key: {} | Saving to MySQL and updating Redis cache", key);
        
        Optional<CacheEntry> existingEntry = cacheRepository.findByKey(key);
        
        if (existingEntry.isPresent()) {
            CacheEntry entry = existingEntry.get();
            entry.setValue(value);
            log.info("📝 Updating existing entry in MySQL for key: {}", key);
            return cacheRepository.save(entry);
        } else {
            CacheEntry newEntry = CacheEntry.builder()
                    .key(key)
                    .value(value)
                    .build();
            log.info("✨ Creating new entry in MySQL for key: {}", key);
            return cacheRepository.save(newEntry);
        }
    }
    
    /**
     * GET operation with Redis caching
     * 
     * @Cacheable: Checks Redis FIRST
     * 
     * Flow:
     * 1. Check Redis cache
     * 2. If FOUND (Cache HIT) → Return immediately ⚡ FAST!
     * 3. If NOT FOUND (Cache MISS) → Query MySQL → Cache in Redis → Return
     * 
     * Performance:
     * - First call: ~50ms (MySQL query)
     * - Subsequent calls: ~5ms (Redis cache) - 10x FASTER!
     * 
     * @param key Cache key to retrieve
     * @return Optional with value if found
     */
    @Cacheable(value = "cacheEntries", key = "#key", unless = "#result == null || !#result.isPresent()")
    public Optional<String> get(String key) {
        log.info("🔍 REDIS GET - Key: {} | Checking Redis first...", key);
        
        // This code only runs on Cache MISS
        log.info("❌ Redis MISS for key: {} | Querying MySQL...", key);
        Optional<CacheEntry> entry = cacheRepository.findByKey(key);
        
        if (entry.isPresent()) {
            log.info("✅ MySQL HIT for key: {} | Caching in Redis for next time", key);
            return Optional.of(entry.get().getValue());
        } else {
            log.info("❌ MySQL MISS for key: {} | Not found anywhere", key);
            return Optional.empty();
        }
    }
    
    /**
     * DELETE operation with cache eviction
     * 
     * @CacheEvict: Removes from Redis after deletion
     * 
     * Flow:
     * 1. Delete from MySQL
     * 2. Evict from Redis cache
     * 3. Return success status
     * 
     * @param key Cache key to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    @CacheEvict(value = "cacheEntries", key = "#key")
    public boolean delete(String key) {
        log.info("🗑️ REDIS DELETE - Key: {} | Removing from MySQL and Redis", key);
        
        if (cacheRepository.existsByKey(key)) {
            cacheRepository.deleteByKey(key);
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
     * @CacheEvict(allEntries = true): Clears entire Redis cache
     * 
     * @return Number of entries deleted
     */
    @Transactional
    @CacheEvict(value = "cacheEntries", allEntries = true)
    public long clearAll() {
        log.info("🧹 Clearing all entries from MySQL and Redis");
        long count = cacheRepository.count();
        cacheRepository.deleteAll();
        log.info("✅ Cleared {} entries from MySQL and Redis", count);
        return count;
    }
}

// Made with Bob
