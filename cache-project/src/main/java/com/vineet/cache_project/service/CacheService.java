package com.vineet.cache_project.service;

import com.vineet.cache_project.entity.CacheEntry;
import com.vineet.cache_project.repository.CacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class containing business logic for cache operations.
 * This layer sits between the Controller and Repository.
 * 
 * @Slf4j: Lombok annotation that provides a logger instance
 * @RequiredArgsConstructor: Lombok annotation that generates constructor for final fields
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {
    
    private final CacheRepository cacheRepository;
    
    /**
     * PUT operation: Store or update a value in the cache.
     * If the key already exists, it updates the value.
     * If the key doesn't exist, it creates a new entry.
     * 
     * @param key The cache key
     * @param value The value to store
     * @return The saved CacheEntry
     */
    @Transactional
    public CacheEntry put(String key, String value) {
        log.info("PUT operation - Key: {}, Value: {}", key, value);
        
        // Check if entry already exists
        Optional<CacheEntry> existingEntry = cacheRepository.findByKey(key);
        
        if (existingEntry.isPresent()) {
            // Update existing entry
            CacheEntry entry = existingEntry.get();
            entry.setValue(value);
            log.info("Updating existing cache entry for key: {}", key);
            return cacheRepository.save(entry);
        } else {
            // Create new entry
            CacheEntry newEntry = CacheEntry.builder()
                    .key(key)
                    .value(value)
                    .build();
            log.info("Creating new cache entry for key: {}", key);
            return cacheRepository.save(newEntry);
        }
    }
    
    /**
     * GET operation: Retrieve a value from the cache by key.
     * 
     * @param key The cache key to look up
     * @return Optional containing the value if found, empty otherwise
     */
    public Optional<String> get(String key) {
        log.info("GET operation - Key: {}", key);
        
        Optional<CacheEntry> entry = cacheRepository.findByKey(key);
        
        if (entry.isPresent()) {
            log.info("Cache HIT for key: {}", key);
            return Optional.of(entry.get().getValue());
        } else {
            log.info("Cache MISS for key: {}", key);
            return Optional.empty();
        }
    }
    
    /**
     * DELETE operation: Remove a cache entry by key.
     * 
     * @param key The cache key to delete
     * @return true if deleted, false if key didn't exist
     */
    @Transactional
    public boolean delete(String key) {
        log.info("DELETE operation - Key: {}", key);
        
        if (cacheRepository.existsByKey(key)) {
            cacheRepository.deleteByKey(key);
            log.info("Successfully deleted cache entry for key: {}", key);
            return true;
        } else {
            log.info("Cache entry not found for key: {}", key);
            return false;
        }
    }
    
    /**
     * Get all cache entries.
     * Useful for debugging and monitoring.
     * 
     * @return List of all cache entries
     */
    public List<CacheEntry> getAllEntries() {
        log.info("Fetching all cache entries");
        return cacheRepository.findAll();
    }
    
    /**
     * Clear all cache entries.
     * 
     * @return Number of entries deleted
     */
    @Transactional
    public long clearAll() {
        log.info("Clearing all cache entries");
        long count = cacheRepository.count();
        cacheRepository.deleteAll();
        log.info("Cleared {} cache entries", count);
        return count;
    }
}

// Made with Bob
