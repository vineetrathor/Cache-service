package com.vineet.cache_project.repository;

import com.vineet.cache_project.entity.CacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for CacheEntry entity.
 * Spring Data JPA automatically implements this interface at runtime.
 * 
 * JpaRepository provides built-in methods:
 * - save(entity): Insert or update
 * - findById(id): Find by primary key
 * - findAll(): Get all entries
 * - deleteById(id): Delete by primary key
 * - count(): Count total entries
 */
@Repository
public interface CacheRepository extends JpaRepository<CacheEntry, Long> {
    
    /**
     * Find a cache entry by its key.
     * Spring Data JPA automatically generates the query:
     * SELECT * FROM cache_entries WHERE cache_key = ?
     * 
     * @param key The cache key to search for
     * @return Optional containing the CacheEntry if found, empty otherwise
     */
    Optional<CacheEntry> findByKey(String key);
    
    /**
     * Check if a cache entry exists with the given key.
     * Spring Data JPA automatically generates:
     * SELECT COUNT(*) > 0 FROM cache_entries WHERE cache_key = ?
     * 
     * @param key The cache key to check
     * @return true if exists, false otherwise
     */
    boolean existsByKey(String key);
    
    /**
     * Delete a cache entry by its key.
     * Spring Data JPA automatically generates:
     * DELETE FROM cache_entries WHERE cache_key = ?
     * 
     * @param key The cache key to delete
     */
    void deleteByKey(String key);
}

// Made with Bob
