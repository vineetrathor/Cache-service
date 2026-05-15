# Cache Patterns Implementation Guide

## Overview

This project implements **4 fundamental caching patterns** using Spring Boot, MySQL, and Redis. Each pattern has different characteristics and is optimized for specific use cases.

---

## 🎯 Quick Pattern Selection Guide

| Your Scenario | Recommended Pattern | Why? |
|--------------|-------------------|------|
| Read-heavy workload, data rarely changes | **Write-Through** | Fast first reads, always consistent |
| Unpredictable access patterns | **Cache-Aside** | Memory efficient, only caches what's needed |
| Write-heavy, data rarely read | **Write-Around** | Prevents cache pollution |
| High write throughput needed | **Write-Back** | Fastest writes, async batch processing |

---

## 1️⃣ Write-Through Pattern

### 📝 Description
Data is written to **both cache and database simultaneously**. Every write operation updates both storage layers immediately.

### 🔄 Flow Diagram
```
PUT Request
    ↓
[Write to MySQL] ← Synchronous
    ↓
[Write to Redis] ← Synchronous
    ↓
Return Response

GET Request
    ↓
[Check Redis] → Hit? Return data
    ↓ Miss
[Read from MySQL]
    ↓
[Cache in Redis]
    ↓
Return Response
```

### ✅ Advantages
- **Fast first reads**: Data is already cached after write
- **Cache consistency**: Cache and DB always in sync
- **Simple logic**: No complex cache management needed
- **Predictable performance**: Consistent read latency

### ❌ Disadvantages
- **Slower writes**: Must wait for both MySQL and Redis
- **Cache pollution**: All data is cached, even if rarely read
- **Higher write latency**: ~50-100ms for writes

### 📊 Performance Characteristics
- **Write latency**: 50-100ms (MySQL + Redis)
- **First read latency**: 5-10ms (Redis hit)
- **Subsequent reads**: 5-10ms (Redis hit)
- **Cache hit ratio**: Very high (100% after first write)

### 🎯 Best Use Cases
- Read-heavy applications (90% reads, 10% writes)
- Data that is frequently accessed after creation
- Applications requiring strong consistency
- User profiles, product catalogs, configuration data

### 💻 Code Example
```java
@Service
public class WriteThroughCacheService {
    
    @CachePut(value = "cacheEntries", key = "#key")
    public String put(String key, String value) {
        // Writes to MySQL first
        CacheEntry entry = new CacheEntry(key, value);
        cacheRepository.save(entry);
        
        // Spring automatically caches in Redis via @CachePut
        return value;
    }
    
    @Cacheable(value = "cacheEntries", key = "#key")
    public String get(String key) {
        // Checks Redis first (via @Cacheable)
        // If miss, loads from MySQL and caches
        return cacheRepository.findById(key)
            .map(CacheEntry::getValue)
            .orElse(null);
    }
}
```

### 🌐 API Endpoints
```bash
# Store data (writes to MySQL + Redis)
PUT http://localhost:8081/api/cache-patterns/write-through/user123
Body: "John Doe"

# Retrieve data (reads from Redis, falls back to MySQL)
GET http://localhost:8081/api/cache-patterns/write-through/user123

# Delete data
DELETE http://localhost:8081/api/cache-patterns/write-through/user123
```

---

## 2️⃣ Cache-Aside Pattern (Lazy Loading)

### 📝 Description
Data is **NOT cached on write**. Cache is populated **lazily** only when data is read. Application manages cache explicitly.

### 🔄 Flow Diagram
```
PUT Request
    ↓
[Write to MySQL ONLY] ← No caching
    ↓
Return Response

GET Request
    ↓
[Check Redis] → Hit? Return data
    ↓ Miss
[Read from MySQL]
    ↓
[Cache in Redis] ← Lazy loading
    ↓
Return Response
```

### ✅ Advantages
- **Memory efficient**: Only caches data that is actually read
- **No cache pollution**: Rarely-read data stays out of cache
- **Fast writes**: Only writes to MySQL
- **Flexible**: Application controls caching logic

### ❌ Disadvantages
- **Slow first read**: Must load from MySQL on first access
- **Cache miss penalty**: First read is 5-10x slower
- **Potential inconsistency**: Cache may be stale if DB is updated externally

### 📊 Performance Characteristics
- **Write latency**: 30-50ms (MySQL only)
- **First read latency**: 50-100ms (MySQL + cache population)
- **Subsequent reads**: 5-10ms (Redis hit)
- **Cache hit ratio**: Grows over time (depends on access patterns)

### 🎯 Best Use Cases
- Unpredictable access patterns
- Large datasets where only subset is frequently accessed
- Memory-constrained environments
- Analytics data, historical records, archive data

### 💻 Code Example
```java
@Service
public class CacheAsideService {
    
    // NO @CachePut - doesn't cache on write
    public String put(String key, String value) {
        // Only writes to MySQL
        CacheEntry entry = new CacheEntry(key, value);
        cacheRepository.save(entry);
        return value;
    }
    
    @Cacheable(value = "cacheAside", key = "#key")
    public String get(String key) {
        // First checks Redis (via @Cacheable)
        // On miss: loads from MySQL and caches automatically
        return cacheRepository.findById(key)
            .map(CacheEntry::getValue)
            .orElse(null);
    }
}
```

### 🌐 API Endpoints
```bash
# Store data (writes to MySQL ONLY, no caching)
PUT http://localhost:8081/api/cache-patterns/cache-aside/user123
Body: "John Doe"

# Retrieve data (lazy loads into cache on first read)
GET http://localhost:8081/api/cache-patterns/cache-aside/user123

# Delete data
DELETE http://localhost:8081/api/cache-patterns/cache-aside/user123
```

---

## 3️⃣ Write-Around Pattern

### 📝 Description
Writes **bypass the cache** completely. Data is cached **selectively** based on access frequency. Only "hot" data (accessed 2+ times) gets cached.

### 🔄 Flow Diagram
```
PUT Request
    ↓
[Write to MySQL ONLY] ← Bypass cache
    ↓
[Invalidate cache if exists]
    ↓
Return Response

GET Request
    ↓
[Check Redis] → Hit? Return data
    ↓ Miss
[Increment access counter]
    ↓
[Read from MySQL]
    ↓
Access count >= 2? → Yes: [Cache in Redis]
    ↓ No: Skip caching
Return Response
```

### ✅ Advantages
- **Prevents cache pollution**: Only frequently-read data is cached
- **Optimal memory usage**: Cache contains only "hot" data
- **Fast writes**: No cache overhead on writes
- **Adaptive**: Automatically identifies hot data

### ❌ Disadvantages
- **Complex logic**: Requires access tracking
- **Slow initial reads**: First 2 reads are uncached
- **Additional overhead**: Access counter management
- **Potential stale cache**: If data is updated frequently

### 📊 Performance Characteristics
- **Write latency**: 30-50ms (MySQL only)
- **First read latency**: 50-100ms (MySQL, no caching)
- **Second read latency**: 50-100ms (MySQL, caches if hot)
- **Subsequent reads**: 5-10ms (Redis hit for hot data)
- **Cache hit ratio**: Lower initially, improves for hot data

### 🎯 Best Use Cases
- Write-heavy workloads with occasional reads
- Data with unpredictable popularity (some items very popular, most rarely accessed)
- Social media posts, news articles, product reviews
- Scenarios where cache space is limited

### 💻 Code Example
```java
@Service
public class WriteAroundService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public String put(String key, String value) {
        // Write to MySQL only
        CacheEntry entry = new CacheEntry(key, value);
        cacheRepository.save(entry);
        
        // Invalidate cache if exists
        redisTemplate.delete("writeAround::" + key);
        redisTemplate.delete("access::" + key);
        
        return value;
    }
    
    public String get(String key) {
        String cacheKey = "writeAround::" + key;
        String accessKey = "access::" + key;
        
        // Check cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Load from MySQL
        String value = cacheRepository.findById(key)
            .map(CacheEntry::getValue)
            .orElse(null);
        
        if (value != null) {
            // Increment access counter
            Long accessCount = redisTemplate.opsForValue().increment(accessKey);
            
            // Cache only if accessed 2+ times
            if (accessCount != null && accessCount >= 2) {
                redisTemplate.opsForValue().set(cacheKey, value, 1, TimeUnit.HOURS);
            }
        }
        
        return value;
    }
}
```

### 🌐 API Endpoints
```bash
# Store data (writes to MySQL, bypasses cache)
PUT http://localhost:8081/api/cache-patterns/write-around/user123
Body: "John Doe"

# Retrieve data (caches only after 2+ accesses)
GET http://localhost:8081/api/cache-patterns/write-around/user123

# Delete data
DELETE http://localhost:8081/api/cache-patterns/write-around/user123
```

---

## 4️⃣ Write-Back Pattern (Write-Behind)

### 📝 Description
Writes go to **cache immediately**, then are **asynchronously batched** to the database. Highest write throughput but risk of data loss.

### 🔄 Flow Diagram
```
PUT Request
    ↓
[Write to Redis] ← Immediate (5ms)
    ↓
[Add to write queue] ← In-memory queue
    ↓
Return Response (fast!)

Background Task (every 30s)
    ↓
[Batch write queued items to MySQL]
    ↓
[Clear write queue]

GET Request
    ↓
[Check Redis] → Hit? Return data
    ↓ Miss
[Read from MySQL]
    ↓
[Cache in Redis]
    ↓
Return Response
```

### ✅ Advantages
- **Fastest writes**: Only writes to Redis (~5ms)
- **Highest throughput**: Can handle massive write loads
- **Batch efficiency**: Reduces DB load via batching
- **Write coalescing**: Multiple updates to same key = 1 DB write

### ❌ Disadvantages
- **Data loss risk**: If Redis crashes before batch write
- **Eventual consistency**: DB lags behind cache by up to 30s
- **Complex recovery**: Need mechanisms to handle failures
- **Not suitable for critical data**: Financial transactions, etc.

### 📊 Performance Characteristics
- **Write latency**: 5-10ms (Redis only)
- **First read latency**: 5-10ms (Redis hit)
- **Subsequent reads**: 5-10ms (Redis hit)
- **DB sync delay**: Up to 30 seconds
- **Cache hit ratio**: Very high (100% for recent writes)

### 🎯 Best Use Cases
- High-frequency writes (logging, metrics, analytics)
- Non-critical data where some loss is acceptable
- Real-time counters, view counts, likes
- Session data, temporary data
- Gaming leaderboards, activity feeds

### 💻 Code Example
```java
@Service
public class WriteBackService {
    
    private final ConcurrentHashMap<String, String> writeQueue = new ConcurrentHashMap<>();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @CachePut(value = "writeBack", key = "#key")
    public String put(String key, String value) {
        // Write to Redis immediately (via @CachePut)
        
        // Add to write queue for batch processing
        writeQueue.put(key, value);
        
        return value;
    }
    
    @Cacheable(value = "writeBack", key = "#key")
    public String get(String key) {
        // Reads from Redis first (via @Cacheable)
        // Falls back to MySQL if not in cache
        return cacheRepository.findById(key)
            .map(CacheEntry::getValue)
            .orElse(null);
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Async
    public void flushWriteQueue() {
        if (writeQueue.isEmpty()) return;
        
        // Batch write to MySQL
        List<CacheEntry> entries = writeQueue.entrySet().stream()
            .map(e -> new CacheEntry(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        
        cacheRepository.saveAll(entries);
        writeQueue.clear();
        
        System.out.println("Flushed " + entries.size() + " entries to MySQL");
    }
}
```

### 🌐 API Endpoints
```bash
# Store data (writes to Redis immediately, queues for MySQL)
PUT http://localhost:8081/api/cache-patterns/write-back/user123
Body: "John Doe"

# Retrieve data (reads from Redis, very fast)
GET http://localhost:8081/api/cache-patterns/write-back/user123

# Delete data
DELETE http://localhost:8081/api/cache-patterns/write-back/user123
```

---

## 📊 Performance Comparison

### Write Performance
| Pattern | Write Latency | Throughput | DB Load |
|---------|--------------|------------|---------|
| Write-Through | 50-100ms | Medium | High (immediate) |
| Cache-Aside | 30-50ms | High | Medium (immediate) |
| Write-Around | 30-50ms | High | Medium (immediate) |
| Write-Back | 5-10ms | **Highest** | Low (batched) |

### Read Performance (First Read)
| Pattern | First Read | Subsequent Reads | Cache Hit Ratio |
|---------|-----------|------------------|-----------------|
| Write-Through | **5-10ms** | 5-10ms | Very High |
| Cache-Aside | 50-100ms | 5-10ms | Medium → High |
| Write-Around | 50-100ms | 5-10ms (hot data) | Low → Medium |
| Write-Back | **5-10ms** | 5-10ms | Very High |

### Memory Efficiency
| Pattern | Memory Usage | Cache Pollution | Best For |
|---------|-------------|-----------------|----------|
| Write-Through | High | Yes (caches all) | Read-heavy |
| Cache-Aside | **Low** | No (lazy) | Unpredictable |
| Write-Around | **Low** | No (selective) | Write-heavy |
| Write-Back | Medium | Some | High throughput |

---

## 🧪 Testing All Patterns

### Using the Comparison Endpoint

```bash
# 1. Store data in all patterns
PUT http://localhost:8081/api/cache-patterns/write-through/test123
PUT http://localhost:8081/api/cache-patterns/cache-aside/test123
PUT http://localhost:8081/api/cache-patterns/write-around/test123
PUT http://localhost:8081/api/cache-patterns/write-back/test123

# 2. Compare performance across all patterns
GET http://localhost:8081/api/cache-patterns/compare/test123

# Response shows performance of each pattern:
{
  "write_through": {
    "value": "test data",
    "duration_ms": 8,
    "characteristics": "Fast first read, immediate caching on write"
  },
  "cache_aside": {
    "value": "test data",
    "duration_ms": 65,
    "characteristics": "Lazy loading, memory efficient"
  },
  "write_around": {
    "value": "test data",
    "duration_ms": 72,
    "characteristics": "Selective caching, prevents pollution"
  },
  "write_back": {
    "value": "test data",
    "duration_ms": 6,
    "characteristics": "Fastest writes, async batch to MySQL"
  },
  "summary": {
    "fastest_read": "write_back",
    "note": "Performance varies based on cache state"
  }
}
```

---

## 🔧 Configuration

All patterns are configured in [`RedisConfig.java`](src/main/java/com/vineet/cache_project/config/RedisConfig.java):

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))
        .serializeValuesWith(/* String serialization */);
    
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put("cacheEntries", config);    // Write-Through
    cacheConfigurations.put("cacheAside", config);      // Cache-Aside
    cacheConfigurations.put("writeAround", config);     // Write-Around
    cacheConfigurations.put("writeBack", config);       // Write-Back
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

---

## 📚 Additional Resources

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/v3/api-docs
- **Performance Logs**: `cache-project/logs.txt`
- **Redis Cache Guide**: [`REDIS_CACHE_GUIDE.md`](REDIS_CACHE_GUIDE.md)
- **Caching Strategies**: [`CACHING_STRATEGIES_EXPLAINED.md`](CACHING_STRATEGIES_EXPLAINED.md)

---

## 🎓 Learning Path

1. **Start with Write-Through**: Simplest pattern, good for understanding basics
2. **Try Cache-Aside**: Learn about lazy loading and memory efficiency
3. **Experiment with Write-Around**: Understand selective caching
4. **Advanced: Write-Back**: Learn async processing and batch operations

---

## ⚠️ Important Notes

### Type Consistency
All cache patterns return `String` type to avoid serialization issues:
```java
// ✅ CORRECT
public String get(String key) { return value; }

// ❌ WRONG - causes ClassCastException
public Optional<String> get(String key) { return Optional.of(value); }
```

### Async Configuration
Write-Back pattern requires async support in [`CacheProjectApplication.java`](src/main/java/com/vineet/cache_project/CacheProjectApplication.java):
```java
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class CacheProjectApplication { }
```

---

**Made with ❤️ for learning cache patterns**