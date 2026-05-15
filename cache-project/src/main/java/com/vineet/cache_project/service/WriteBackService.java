package com.vineet.cache_project.service;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WRITE-BACK PATTERN (Write-Behind)
 * 
 * Pattern: Write-Back / Write-Behind (Async Writes)
 * 
 * CHARACTERISTICS:
 * - Writes go to cache IMMEDIATELY (fast!)
 * - Database writes are ASYNCHRONOUS (queued)
 * - Batch writes to database periodically
 * - Fastest write performance
 * - Risk of data loss if cache fails before DB write
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────┐
 * │  Client Request                                      │
 * └────────────────┬────────────────────────────────────┘
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 1: Redis Cache (In-Memory) - FAST (~5ms)     │
 * │  - Immediate writes                                 │
 * │  - Write queue for DB sync                          │
 * │  - Fastest write performance                        │
 * └────────────────┬────────────────────────────────────┘
 *                  │ Async batch writes
 *                  ▼
 * ┌─────────────────────────────────────────────────────┐
 * │  TIER 2: MySQL Database - PERSISTENT (~50ms)       │
 * │  - Eventual consistency                             │
 * │  - Batch writes every 30 seconds                    │
 * └─────────────────────────────────────────────────────┘
 * 
 * FLOW:
 * PUT: Redis (cache immediately) → Queue for DB → Return (FAST!)
 * GET: Redis (check) → HIT → Return (fast!)
 * GET: Redis (check) → MISS → MySQL → Redis → Return
 * Background: Process queue → Batch write to MySQL
 * DELETE: MySQL (delete) → Redis (evict) → Return
 * 
 * USE CASES:
 * - Write-heavy workloads (social media posts, logs)
 * - When write speed is critical
 * - Acceptable eventual consistency
 * - High-throughput systems
 * 
 * PROS:
 * ✅ Fastest write performance (~5ms)
 * ✅ Reduced database load (batch writes)
 * ✅ Better throughput
 * ✅ Immediate cache availability
 * 
 * CONS:
 * ❌ Risk of data loss (if cache fails before DB write)
 * ❌ Eventual consistency (not immediate)
 * ❌ Complex implementation
 * ❌ Requires careful error handling
 * 
 * CRITICAL: Returns String type to avoid serialization issues!
 */
@Service
@Slf4j
@RequiredArgsConstructor
@EnableAsync
public class WriteBackService {
    
    private final CacheRepository cacheRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Write queue for async database writes
    private final Map<String, String> writeQueue = new ConcurrentHashMap<>();
    private static final String CACHE_PREFIX = "writeBack::";
    
    /**
     * PUT operation with immediate caching and async DB write (Write-Back pattern)
     * 
     * Flow:
     * 1. Cache in Redis IMMEDIATELY (fast!)
     * 2. Add to write queue for async DB write
     * 3. Return immediately (don't wait for DB)
     * 4. Background job writes to DB later
     * 
     * Why this is fastest:
     * - Client gets response in ~5ms (Redis only)
     * - No waiting for MySQL (~50ms saved!)
     * - Database writes batched for efficiency
     * 
     * Trade-off:
     * - Data in Redis but not yet in MySQL (eventual consistency)
     * - If Redis crashes before DB write, data is lost
     * - Need monitoring and error handling
     * 
     * @param key Cache key
     * @param value Value to store
     * @return String value (for consistent type with GET)
     */
    public String put(String key, String value) {
        log.info("🔵 WRITE-BACK PUT - Key: {} | Caching immediately, queuing DB write", key);
        
        // Step 1: Cache in Redis IMMEDIATELY (fast!)
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, value, 10, TimeUnit.MINUTES);
        log.info("✅ Cached in Redis immediately (~5ms)");
        
        // Step 2: Add to write queue for async DB write
        writeQueue.put(key, value);
        log.info("📝 Added to write queue (will sync to MySQL in background)");
        
        // Step 3: Return immediately (don't wait for DB!)
        log.info("⚡ Returning immediately - DB write will happen asynchronously");
        return value; // Return String type (consistent with GET)
    }
    
    /**
     * Background job to flush write queue to database
     * 
     * Runs every 30 seconds to batch write queued data to MySQL
     * 
     * Flow:
     * 1. Get all queued writes
     * 2. Batch write to MySQL
     * 3. Clear queue
     * 4. Log results
     * 
     * Why batch writes?
     * - More efficient than individual writes
     * - Reduces database load
     * - Better throughput
     * 
     * Error handling:
     * - Failed writes stay in queue
     * - Retry on next cycle
     * - Log errors for monitoring
     */
    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    @Async
    @Transactional
    public void flushWriteQueue() {
        if (writeQueue.isEmpty()) {
            return;
        }
        
        log.info("🔄 WRITE-BACK FLUSH - Processing {} queued writes to MySQL", writeQueue.size());
        
        int successCount = 0;
        int failCount = 0;
        
        // Create a copy to avoid concurrent modification
        Map<String, String> queueCopy = new HashMap<>(writeQueue);
        
        for (Map.Entry<String, String> entry : queueCopy.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            try {
                // Write to MySQL
                Optional<CacheEntry> existingEntry = cacheRepository.findByKey(key);
                
                if (existingEntry.isPresent()) {
                    CacheEntry cacheEntry = existingEntry.get();
                    cacheEntry.setValue(value);
                    cacheRepository.save(cacheEntry);
                    log.debug("📝 Updated MySQL for key: {}", key);
                } else {
                    CacheEntry newEntry = CacheEntry.builder()
                            .key(key)
                            .value(value)
                            .build();
                    cacheRepository.save(newEntry);
                    log.debug("✨ Created MySQL entry for key: {}", key);
                }
                
                // Remove from queue after successful write
                writeQueue.remove(key);
                successCount++;
                
            } catch (Exception e) {
                log.error("❌ Failed to write key {} to MySQL: {}", key, e.getMessage());
                failCount++;
                // Keep in queue for retry
            }
        }
        
        log.info("✅ WRITE-BACK FLUSH Complete - Success: {}, Failed: {}", successCount, failCount);
    }
    
    /**
     * GET operation with caching (Write-Back pattern)
     * 
     * @Cacheable: Checks Redis FIRST
     * 
     * Flow:
     * 1. Check Redis cache
     * 2. If FOUND (Cache HIT) → Return immediately ⚡ FAST! (~5ms)
     * 3. If NOT FOUND (Cache MISS) → Query MySQL (~50ms)
     * 4. Cache result in Redis for next time
     * 5. Return value
     * 
     * Note: Data might be in Redis but not yet in MySQL (eventual consistency)
     * 
     * CRITICAL: Returns String (not Optional<String>) to avoid serialization issues!
     * 
     * @param key Cache key to retrieve
     * @return String value if found, null if not found
     */
    @Cacheable(value = "writeBack", key = "#key", unless = "#result == null")
    public String get(String key) {
        log.info("🔍 WRITE-BACK GET - Key: {} | Checking Redis first...", key);
        
        // This code only runs on Cache MISS
        log.info("❌ Redis MISS for key: {} | Querying MySQL...", key);
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
     * 2. Evict from Redis cache
     * 3. Remove from write queue (if present)
     * 4. Return success status
     * 
     * @param key Cache key to delete
     * @return true if deleted, false if not found
     */
    @Transactional
    @CacheEvict(value = "writeBack", key = "#key")
    public boolean delete(String key) {
        log.info("🗑️ WRITE-BACK DELETE - Key: {} | Removing from MySQL, Redis, and queue", key);
        
        boolean deleted = false;
        
        // Delete from MySQL
        if (cacheRepository.existsByKey(key)) {
            cacheRepository.deleteByKey(key);
            deleted = true;
        }
        
        // Remove from write queue
        writeQueue.remove(key);
        
        // Remove from Redis cache
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
        
        if (deleted) {
            log.info("✅ Successfully deleted from MySQL, Redis, and queue: {}", key);
        } else {
            log.info("❌ Key not found: {}", key);
        }
        
        return deleted;
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
     * Clears MySQL, Redis cache, and write queue
     * 
     * @return Number of entries deleted
     */
    @Transactional
    @CacheEvict(value = "writeBack", allEntries = true)
    public long clearAll() {
        log.info("🧹 Clearing all entries from MySQL, Redis, and queue");
        
        long count = cacheRepository.count();
        cacheRepository.deleteAll();
        
        // Clear write queue
        writeQueue.clear();
        
        // Clear Redis cache
        redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
        
        log.info("✅ Cleared {} entries from MySQL, Redis, and queue", count);
        return count;
    }
    
    /**
     * Get current write queue size (for monitoring)
     * 
     * @return Number of pending writes
     */
    public int getQueueSize() {
        return writeQueue.size();
    }
}

// Made with Bob