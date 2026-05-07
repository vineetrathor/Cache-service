package com.vineet.cache_project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing a cache entry in the database.
 * Each entry has a unique key and stores a value along with timestamps.
 */
@Entity
@Table(name = "cache_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cache_key", unique = true, nullable = false, length = 255)
    private String key;
    
    @Column(name = "cache_value", columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Automatically set timestamps before persisting to database
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Automatically update the updatedAt timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// Made with Bob
