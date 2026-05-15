package com.vineet.cache_project.service;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CACHE-ASIDE PATTERN (Lazy Loading)
 * 
 * Pattern: Cache-Aside / Lazy Loading
 * 
 * CHARACTERISTICS:
 * - Application manages cache explicitly
 * - Data loaded into cache ONLY when read
 * - Writes go ONLY to database (cache NOT updated)
 * - Cache populated on first read (lazy)
 * - Memory efficient (only caches what's read)
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────┐
 * │  Client Request                                      │
 * └────────────────┬────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 1: Redis Cache (In-Memory) - FAST (~5ms)     │
 * │  - Populated ONLY on reads                          │
 * │  - TTL: 10 minutes                                  │
 * │  - Memory efficient                                 │
 * └────────────────┬────────────────────────────────────┘
 *                  │ Cache MISS
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 2: MySQL Database - PERSISTENT (~50ms)       │
 * │  - Source of truth                                  │
 * │  - Always up-to-date                                │
 * └─────────────────────────────────────────────────────┘
 * 
 * FLOW:
 * PUT: MySQL (save) → Return (NO caching)
 * GET: Redis (check) → MISS → MySQL (query) → Redis (cache) → Return
 * GET: Redis (check) → HIT → Return (fast!)
 * DELETE: MySQL (delete) → Redis (evict) → Return
 * 
 * USE CASES:
 * - Read-heavy workloads with unpredictable access patterns
 * - Data that's written frequently but read occasionally
 * - Memory-constrained environments
 * - When you want to avoid caching unused data
 * 
 * PROS:
 * ✅ Memory efficient (only caches read data)
 * ✅ No wasted cache space
 * ✅ Simple to implement
 * ✅ Cache failures don't affect writes
 * 
 * CONS:
 * ❌ First read is slow (cache miss)
 * ❌ Cache stampede risk (many requests for same uncached data)
 * ❌ Stale data possible (if DB updated externally)
 * 
 * CRITICAL: Returns String type to avoid serialization issues!
 * - PUT returns String (the value saved)
 * - GET returns String (from cache or DB)
 * - Both methods cache and return the SAME TYPE
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheAsideService {
    
    private final CacheRepository cacheRepository;
    
    /**
     * PUT operation WITHOUT caching (Cache-Aside pattern)
     * 
     * NO @CachePut annotation - cache is NOT updated on write
     * 
     * Flow:
     * 1. Save to MySQL (persistent storage)
     * 2. Return value (Redis NOT touched)
     * 3. Cache will be populated on first GET
     * 
     * Why no caching on PUT?
     * - Memory efficient: Don't cache data that might never be read
     * - Lazy loading: Cache only when actually needed
     * - Avoids cache pollution
     * 
     * @param key Cache key
     * @param value Value to store
     * @return String value (for consistent type with GET)
     */
    @Transactional
    public String put(String key, String value) {
        log.info("🔵 CACHE-ASIDE PUT - Key: {} | Saving to MySQL ONLY (no caching)", key);
        
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
        
        log.info("✅ Saved to MySQL. Redis NOT updated (will cache on first GET)");
        return value; // Return String type (consistent with GET)
    }
    
    /**
     * GET operation with lazy caching (Cache-Aside pattern)
     * 
     * @Cacheable: Checks Redis FIRST, caches on miss
     * 
     * Flow:
     * 1. Check Redis cache
     * 2. If FOUND (Cache HIT) → Return immediately ⚡ FAST! (~5ms)
     * 3. If NOT FOUND (Cache MISS) → Query MySQL (~50ms)
     * 4. Cache result in Redis for next time
     * 5. Return value
     * 
     * Performance:
     * - First call after PUT: ~50ms (MySQL query + cache)
     * - Subsequent calls: ~5ms (Redis cache) - 10x FASTER!
     * 
     * Difference from Write-Through:
     * - Write-Through: First GET is fast (already cached on PUT)
     * - Cache-Aside: First GET is slow (cache miss, then cached)
     * 
     * CRITICAL: Returns String (not Optional<String>) to avoid serialization issues!
     * 
     * @param key Cache key to retrieve
     * @return String value if found, null if not found
     */
    @Cacheable(value = "cacheAside", key = "#key", unless = "#result == null")
    public String get(String key) {
        log.info("🔍 CACHE-ASIDE GET - Key: {} | Checking Redis first...", key);
        
        // This code only runs on Cache MISS
        log.info("❌ Redis MISS for key: {} | Querying MySQL (lazy loading)...", key);
        Optional<CacheEntry> entry = cacheRepository.findByKey(key);
        
        if (entry.isPresent()) {
            String value = entry.get().getValue();
            log.info("✅ MySQL HIT for key: {} | Caching in Redis for next time", key);
            return value; // Spring Cache will cache this String in Redis
        } else {
            log.info("❌ MySQL MISS for key: {} | Not found anywhere", key);
            return null; // unless="#result == null" prevents caching null
        }
    }
    
    /**
     * DELETE operation with cache eviction
     * 
     * @CacheEvict: Removes from Redis after deletion
     * 
     * Flow:
     * 1. Delete from MySQL
     * 2. Evict from Redis cache (if present)
     * 3. Return success status
     * 
     * @param key Cache key to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    @CacheEvict(value = "cacheAside", key = "#key")
    public boolean delete(String key) {
        log.info("🗑️ CACHE-ASIDE DELETE - Key: {} | Removing from MySQL and Redis", key);
        
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
    @CacheEvict(value = "cacheAside", allEntries = true)
    public long clearAll() {
        log.info("🧹 Clearing all entries from MySQL and Redis");
        long count = cacheRepository.count();
        cacheRepository.deleteAll();
        log.info("✅ Cleared {} entries from MySQL and Redis", count);
        return count;
    }
}

// Made with Bob