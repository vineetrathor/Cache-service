package com.vineet.cache_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cache Service Application with Multiple Cache Patterns
 *
 * Implements 4 core caching patterns:
 * 1. Write-Through: Cache on write, fast reads
 * 2. Cache-Aside: Lazy loading, memory efficient
 * 3. Write-Around: Selective caching, prevents pollution
 * 4. Write-Back: Async writes, fastest performance
 *
 * @EnableAsync: Enables async method execution for Write-Back pattern
 * @EnableScheduling: Enables scheduled tasks for Write-Back batch writes
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CacheProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CacheProjectApplication.class, args);
	}

}
