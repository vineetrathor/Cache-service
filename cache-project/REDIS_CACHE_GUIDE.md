# Redis Cache Integration Guide

## 📚 Table of Contents
1. [What is Redis and Why Use It?](#what-is-redis-and-why-use-it)
2. [Architecture Comparison](#architecture-comparison)
3. [Performance Comparison](#performance-comparison)
4. [How to Test](#how-to-test)
5. [Understanding Cache Behavior](#understanding-cache-behavior)
6. [Redis Commands](#redis-commands)
7. [Configuration Details](#configuration-details)
8. [Troubleshooting](#troubleshooting)

---

## 🚀 What is Redis and Why Use It?

### What is Redis?

**Redis** (Remote Dictionary Server) is an in-memory data structure store used as:
- **Cache**: Store frequently accessed data in memory
- **Database**: Fast key-value storage
- **Message Broker**: Pub/sub messaging

### Why Use Redis for Caching?

| Feature | MySQL (Database) | Redis (Cache) |
|---------|------------------|---------------|
| **Speed** | ~50ms per query | ~5ms per query |
| **Storage** | Disk (persistent) | Memory (volatile) |
| **Use Case** | Permanent data | Temporary/frequent data |
| **Cost** | Slower but reliable | Faster but temporary |

### The Problem We're Solving

**Without Redis:**
```
Every request → MySQL query → 50ms response time
```

**With Redis:**
```
First request → MySQL query → Cache in Redis → 50ms
Subsequent requests → Redis cache → 5ms (10x FASTER!)
```

---

## 🏗️ Architecture Comparison

### Original Architecture (Without Redis)

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP Request
       ▼
┌─────────────────────┐
│   Controller        │
│  /api/cache/*       │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   Service Layer     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│   MySQL Database    │
│   (~50ms per query) │
└─────────────────────┘
```

**Every request hits the database!**

### New Architecture (With Redis)

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP Request
       ▼
┌──────────────────────────┐
│   Controller             │
│  /api/redis-cache/*      │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│   Service Layer          │
│   (with @Cacheable)      │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│   Redis Cache            │
│   (~5ms)                 │
│   ┌──────────────────┐   │
│   │ Cache HIT?       │   │
│   │ YES → Return     │   │
│   │ NO  → Query DB   │   │
│   └──────────────────┘   │
└──────┬───────────────────┘
       │ Cache MISS only
       ▼
┌──────────────────────────┐
│   MySQL Database         │
│   (~50ms)                │
└──────────────────────────┘
```

**First request hits DB, subsequent requests hit cache!**

---

## ⚡ Performance Comparison

### Endpoint Comparison

| Endpoint | Caching | First Call | Subsequent Calls | Improvement |
|----------|---------|------------|------------------|-------------|
| `/api/cache/{key}` | ❌ No | ~50ms | ~50ms | - |
| `/api/redis-cache/{key}` | ✅ Yes | ~50ms | ~5ms | **10x faster** |

### Real-World Impact

**Scenario: 1000 requests for the same data**

**Without Redis:**
```
1000 requests × 50ms = 50,000ms (50 seconds)
1000 database queries
```

**With Redis:**
```
1 request × 50ms (MySQL) = 50ms
999 requests × 5ms (Redis) = 4,995ms
Total: 5,045ms (5 seconds)

90% faster! 🚀
```

---

## 🧪 How to Test

### Step 1: Start the Application

Make sure both are running:
1. **MySQL** (port 3306)
2. **Redis** (port 6379 in Docker)
3. **Spring Boot** (port 8080)

```bash
# Check Redis is running
docker ps

# Should see:
# redis-cache   redis:latest   Up   0.0.0.0:6379->6379/tcp
```

### Step 2: Open Swagger UI

Navigate to: http://localhost:8080/swagger-ui.html

You'll see TWO controller sections:
- **Cache Controller** (original, no Redis)
- **Redis Cache Controller** (new, with Redis)

### Step 3: Test WITHOUT Redis

1. **PUT** data:
   - Endpoint: `PUT /api/cache/user1`
   - Body:
   ```json
   {
     "value": "John Doe"
   }
   ```
   - Click "Execute"
   - Note the response time

2. **GET** data (First time):
   - Endpoint: `GET /api/cache/user1`
   - Click "Execute"
   - Check response time: ~50ms
   - Check console logs

3. **GET** data (Second time):
   - Click "Execute" again
   - Check response time: Still ~50ms
   - **No improvement!**

### Step 4: Test WITH Redis

1. **PUT** data:
   - Endpoint: `PUT /api/redis-cache/user2`
   - Body:
   ```json
   {
     "value": "Jane Smith"
   }
   ```
   - Click "Execute"
   - Note the response time

2. **GET** data (First time):
   - Endpoint: `GET /api/redis-cache/user2`
   - Click "Execute"
   - Check response time: ~50ms
   - Check console logs: "❌ Redis MISS"

3. **GET** data (Second time):
   - Click "Execute" again
   - Check response time: ~5ms ⚡
   - Check console logs: "✅ Redis HIT"
   - **10x faster!**

4. **GET** data (Multiple times):
   - Keep clicking "Execute"
   - Every call is fast (~5ms)
   - All from Redis cache!

### Step 5: Compare Response Times

Check the `X-Response-Time` header in responses:

**Without Redis:**
```
X-Response-Time: 47ms
X-Response-Time: 52ms
X-Response-Time: 49ms
```

**With Redis (after first call):**
```
X-Response-Time: 5ms
X-Response-Time: 4ms
X-Response-Time: 6ms
```

---

## 🔍 Understanding Cache Behavior

### Cache Lifecycle

```
┌─────────────────────────────────────────────────────┐
│  1. PUT /api/redis-cache/key1                       │
│     → Save to MySQL                                 │
│     → Store in Redis                                │
│     → TTL: 10 minutes                               │
└─────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│  2. GET /api/redis-cache/key1 (First time)          │
│     → Check Redis: NOT FOUND (Cache MISS)           │
│     → Query MySQL: FOUND                            │
│     → Store in Redis                                │
│     → Return value (~50ms)                          │
└─────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│  3. GET /api/redis-cache/key1 (Subsequent times)    │
│     → Check Redis: FOUND (Cache HIT)                │
│     → Return immediately (~5ms)                     │
│     → MySQL not queried!                            │
└─────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│  4. After 10 minutes (TTL expires)                  │
│     → Redis automatically removes entry             │
│     → Next GET will be Cache MISS again             │
│     → Cycle repeats                                 │
└─────────────────────────────────────────────────────┘
```

### Spring Cache Annotations

#### @Cacheable
```java
@Cacheable(value = "cacheEntries", key = "#key")
public Optional<String> get(String key) {
    // This code only runs on Cache MISS
    return queryDatabase(key);
}
```

**Behavior:**
- Checks Redis before executing method
- If found: Returns cached value (method not executed)
- If not found: Executes method, caches result, returns value

#### @CachePut
```java
@CachePut(value = "cacheEntries", key = "#key")
public CacheEntry put(String key, String value) {
    // Always executes and updates cache
    return saveToDatabase(key, value);
}
```

**Behavior:**
- Always executes the method
- Updates Redis cache with return value
- Used for PUT/UPDATE operations

#### @CacheEvict
```java
@CacheEvict(value = "cacheEntries", key = "#key")
public boolean delete(String key) {
    // Removes from cache after execution
    return deleteFromDatabase(key);
}
```

**Behavior:**
- Executes the method
- Removes entry from Redis cache
- Used for DELETE operations

---

## 🛠️ Redis Commands

### Accessing Redis CLI

```bash
# Connect to Redis container
docker exec -it redis-cache redis-cli

# You'll see:
# 127.0.0.1:6379>
```

### Useful Commands

```bash
# List all keys
KEYS *

# Get a specific value
GET "cacheEntries::user1"

# Check if key exists
EXISTS "cacheEntries::user1"

# Get TTL (time to live) in seconds
TTL "cacheEntries::user1"

# Delete a key
DEL "cacheEntries::user1"

# Clear all cache
FLUSHALL

# Get info about Redis
INFO

# Monitor all commands in real-time
MONITOR

# Exit Redis CLI
EXIT
```

### Example Session

```bash
$ docker exec -it redis-cache redis-cli

127.0.0.1:6379> KEYS *
1) "cacheEntries::user1"
2) "cacheEntries::user2"

127.0.0.1:6379> GET "cacheEntries::user1"
"{\"key\":\"user1\",\"value\":\"John Doe\",...}"

127.0.0.1:6379> TTL "cacheEntries::user1"
(integer) 543

127.0.0.1:6379> DEL "cacheEntries::user1"
(integer) 1

127.0.0.1:6379> EXIT
```

---

## ⚙️ Configuration Details

### application.properties

```properties
# Redis Connection
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000  # 10 minutes in milliseconds
```

### RedisConfig.java

Key configurations:
- **TTL**: 10 minutes (600,000ms)
- **Serialization**: JSON format
- **Null values**: Not cached
- **Key format**: `cacheEntries::{key}`

### Docker Redis Container

```bash
# Start Redis
docker run -d --name redis-cache -p 6379:6379 redis:latest

# Stop Redis
docker stop redis-cache

# Start existing container
docker start redis-cache

# Remove container
docker rm redis-cache
```

---

## 🐛 Troubleshooting

### Issue: Redis connection refused

**Symptoms:**
```
Unable to connect to Redis at localhost:6379
```

**Solution:**
```bash
# Check if Redis is running
docker ps

# If not running, start it
docker start redis-cache

# If container doesn't exist, create it
docker run -d --name redis-cache -p 6379:6379 redis:latest
```

### Issue: Cache not working

**Symptoms:**
- Every request shows "Redis MISS"
- Response times always ~50ms

**Checklist:**
1. ✅ Redis container is running: `docker ps`
2. ✅ `@EnableCaching` is present in RedisConfig
3. ✅ Using `/api/redis-cache/*` endpoints (not `/api/cache/*`)
4. ✅ Check application logs for errors

### Issue: Stale data in cache

**Symptoms:**
- Updated data in MySQL but old data returned

**Solution:**
```bash
# Option 1: Wait for TTL to expire (10 minutes)

# Option 2: Clear specific key via API
DELETE /api/redis-cache/{key}

# Option 3: Clear all cache via API
DELETE /api/redis-cache

# Option 4: Clear Redis manually
docker exec -it redis-cache redis-cli FLUSHALL
```

### Issue: Port 6379 already in use

**Symptoms:**
```
Error starting userland proxy: listen tcp4 0.0.0.0:6379: bind: address already in use
```

**Solution:**
```bash
# Find what's using port 6379
netstat -ano | findstr :6379

# Stop the process or use different port
docker run -d --name redis-cache -p 6380:6379 redis:latest

# Update application.properties
spring.data.redis.port=6380
```

---

## 📊 Performance Monitoring

### Console Logs

Watch for these log patterns:

**Cache MISS (First call):**
```
🔍 REDIS GET - Key: user1 | Checking Redis first...
❌ Redis MISS for key: user1 | Querying MySQL...
✅ MySQL HIT for key: user1 | Caching in Redis for next time
✅ Request Completed: GET /api/redis-cache/user1 - Status: 200 - Duration: 47ms
```

**Cache HIT (Subsequent calls):**
```
🔍 REDIS GET - Key: user1 | Checking Redis first...
✅ Request Completed: GET /api/redis-cache/user1 - Status: 200 - Duration: 5ms
```

Notice: No "Redis MISS" or "MySQL" logs = Data served from Redis!

### Response Headers

Check `X-Response-Time` header:
- First call: ~50ms
- Cached calls: ~5ms

---

## 🎯 Best Practices

### When to Use Redis Cache

✅ **Good Use Cases:**
- Frequently accessed data
- Data that doesn't change often
- Read-heavy workloads
- User sessions
- API responses
- Configuration data

❌ **Bad Use Cases:**
- Data that changes frequently
- Write-heavy workloads
- Data that must be 100% consistent
- Large binary files
- Data that's rarely accessed

### Cache Strategy Tips

1. **Set appropriate TTL**
   - Too short: Cache misses, poor performance
   - Too long: Stale data
   - Current: 10 minutes (good for demo)

2. **Monitor cache hit rate**
   - Good: >80% cache hits
   - Poor: <50% cache hits

3. **Handle cache failures gracefully**
   - If Redis is down, fall back to MySQL
   - Don't let cache failures break your app

4. **Clear cache on updates**
   - Use `@CachePut` for updates
   - Use `@CacheEvict` for deletes

---

## 🚀 Next Steps

### Enhancements You Can Add

1. **Cache Statistics**
   - Track hit/miss ratio
   - Monitor cache size
   - Alert on low hit rates

2. **Multiple Cache Regions**
   - Different TTLs for different data types
   - Separate caches for users, products, etc.

3. **Cache Warming**
   - Pre-load frequently accessed data
   - Reduce initial cache misses

4. **Distributed Caching**
   - Multiple app instances sharing Redis
   - Session management across servers

5. **Cache Invalidation Strategies**
   - Time-based (TTL)
   - Event-based (on data change)
   - Manual (admin endpoint)

---

## 📚 Additional Resources

- [Redis Official Documentation](https://redis.io/documentation)
- [Spring Cache Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Commands Reference](https://redis.io/commands)

---

**Happy Caching! 🚀**