# Quick Testing Guide - Cache Patterns

## 🚀 Prerequisites

1. **Start MySQL** (port 3306)
2. **Start Redis** (port 6379)
3. **Run the application**:
   ```bash
   cd cache-project
   mvn spring-boot:run
   ```
4. **Verify health**:
   ```bash
   curl http://localhost:8081/api/cache-patterns/health
   ```

---

## 📋 Test Scenarios

### Scenario 1: Write Performance Comparison

Test how fast each pattern handles writes.

```bash
# Write-Through (slowest - writes to MySQL + Redis)
curl -X PUT http://localhost:8081/api/cache-patterns/write-through/user1 \
  -H "Content-Type: text/plain" \
  -d "Alice"

# Cache-Aside (fast - writes to MySQL only)
curl -X PUT http://localhost:8081/api/cache-patterns/cache-aside/user1 \
  -H "Content-Type: text/plain" \
  -d "Alice"

# Write-Around (fast - writes to MySQL only)
curl -X PUT http://localhost:8081/api/cache-patterns/write-around/user1 \
  -H "Content-Type: text/plain" \
  -d "Alice"

# Write-Back (fastest - writes to Redis only, queues for MySQL)
curl -X PUT http://localhost:8081/api/cache-patterns/write-back/user1 \
  -H "Content-Type: text/plain" \
  -d "Alice"
```

**Expected Results**:
- Write-Through: ~50-100ms (MySQL + Redis)
- Cache-Aside: ~30-50ms (MySQL only)
- Write-Around: ~30-50ms (MySQL only)
- Write-Back: ~5-10ms (Redis only) ⚡ **FASTEST**

---

### Scenario 2: First Read Performance

Test how fast the **first read** is after writing data.

```bash
# Write data first
curl -X PUT http://localhost:8081/api/cache-patterns/write-through/user2 \
  -H "Content-Type: text/plain" -d "Bob"

curl -X PUT http://localhost:8081/api/cache-patterns/cache-aside/user2 \
  -H "Content-Type: text/plain" -d "Bob"

curl -X PUT http://localhost:8081/api/cache-patterns/write-around/user2 \
  -H "Content-Type: text/plain" -d "Bob"

curl -X PUT http://localhost:8081/api/cache-patterns/write-back/user2 \
  -H "Content-Type: text/plain" -d "Bob"

# Now read immediately (first read)
curl http://localhost:8081/api/cache-patterns/write-through/user2
curl http://localhost:8081/api/cache-patterns/cache-aside/user2
curl http://localhost:8081/api/cache-patterns/write-around/user2
curl http://localhost:8081/api/cache-patterns/write-back/user2
```

**Expected Results**:
- Write-Through: ~5-10ms ⚡ (already cached)
- Cache-Aside: ~50-100ms 🐌 (loads from MySQL, then caches)
- Write-Around: ~50-100ms 🐌 (loads from MySQL, no caching yet)
- Write-Back: ~5-10ms ⚡ (already cached)

---

### Scenario 3: Subsequent Read Performance

Test how fast **subsequent reads** are (after first read).

```bash
# Read the same key multiple times
for i in {1..5}; do
  echo "Read #$i:"
  curl http://localhost:8081/api/cache-patterns/cache-aside/user2
  echo ""
done
```

**Expected Results**:
- All patterns: ~5-10ms ⚡ (all cached after first read)

---

### Scenario 4: Write-Around Selective Caching

Test how Write-Around only caches "hot" data (accessed 2+ times).

```bash
# Write data
curl -X PUT http://localhost:8081/api/cache-patterns/write-around/product1 \
  -H "Content-Type: text/plain" -d "Laptop"

# First read (not cached, access count = 1)
curl http://localhost:8081/api/cache-patterns/write-around/product1
# Expected: ~50-100ms (MySQL read, NOT cached)

# Second read (still not cached, access count = 2, NOW caches)
curl http://localhost:8081/api/cache-patterns/write-around/product1
# Expected: ~50-100ms (MySQL read, NOW cached)

# Third read (cached!)
curl http://localhost:8081/api/cache-patterns/write-around/product1
# Expected: ~5-10ms (Redis hit) ⚡
```

---

### Scenario 5: Write-Back Async Behavior

Test how Write-Back queues writes and flushes every 30 seconds.

```bash
# Write multiple items quickly
curl -X PUT http://localhost:8081/api/cache-patterns/write-back/metric1 \
  -H "Content-Type: text/plain" -d "100"

curl -X PUT http://localhost:8081/api/cache-patterns/write-back/metric2 \
  -H "Content-Type: text/plain" -d "200"

curl -X PUT http://localhost:8081/api/cache-patterns/write-back/metric3 \
  -H "Content-Type: text/plain" -d "300"

# Read immediately (from Redis, very fast)
curl http://localhost:8081/api/cache-patterns/write-back/metric1
# Expected: ~5-10ms ⚡

# Check MySQL directly (may not be there yet!)
# Wait 30 seconds, then check again - data will be flushed
```

**Watch the console logs**:
```
Flushed 3 entries to MySQL
```

---

### Scenario 6: Compare All Patterns

Use the comparison endpoint to test all patterns at once.

```bash
# Store data in all patterns
curl -X PUT http://localhost:8081/api/cache-patterns/write-through/test123 \
  -H "Content-Type: text/plain" -d "Test Data"

curl -X PUT http://localhost:8081/api/cache-patterns/cache-aside/test123 \
  -H "Content-Type: text/plain" -d "Test Data"

curl -X PUT http://localhost:8081/api/cache-patterns/write-around/test123 \
  -H "Content-Type: text/plain" -d "Test Data"

curl -X PUT http://localhost:8081/api/cache-patterns/write-back/test123 \
  -H "Content-Type: text/plain" -d "Test Data"

# Compare all patterns
curl http://localhost:8081/api/cache-patterns/compare/test123 | jq
```

**Expected Response**:
```json
{
  "write_through": {
    "value": "Test Data",
    "duration_ms": 8,
    "characteristics": "Fast first read, immediate caching on write"
  },
  "cache_aside": {
    "value": "Test Data",
    "duration_ms": 65,
    "characteristics": "Lazy loading, memory efficient"
  },
  "write_around": {
    "value": "Test Data",
    "duration_ms": 72,
    "characteristics": "Selective caching, prevents pollution"
  },
  "write_back": {
    "value": "Test Data",
    "duration_ms": 6,
    "characteristics": "Fastest writes, async batch to MySQL"
  },
  "summary": {
    "fastest_read": "write_back"
  }
}
```

---

### Scenario 7: Cache Pollution Test

Test how Cache-Aside and Write-Around prevent cache pollution.

```bash
# Write 100 items to each pattern
for i in {1..100}; do
  curl -X PUT http://localhost:8081/api/cache-patterns/write-through/item$i \
    -H "Content-Type: text/plain" -d "Data$i" -s > /dev/null
  
  curl -X PUT http://localhost:8081/api/cache-patterns/cache-aside/item$i \
    -H "Content-Type: text/plain" -d "Data$i" -s > /dev/null
done

# Check Redis memory usage
redis-cli INFO memory | grep used_memory_human
```

**Expected**:
- Write-Through: All 100 items cached (higher memory)
- Cache-Aside: 0 items cached (lower memory) - only caches on read

---

### Scenario 8: Delete Operations

Test delete operations across all patterns.

```bash
# Delete from each pattern
curl -X DELETE http://localhost:8081/api/cache-patterns/write-through/user1
curl -X DELETE http://localhost:8081/api/cache-patterns/cache-aside/user1
curl -X DELETE http://localhost:8081/api/cache-patterns/write-around/user1
curl -X DELETE http://localhost:8081/api/cache-patterns/write-back/user1

# Verify deletion
curl http://localhost:8081/api/cache-patterns/write-through/user1
# Expected: {"value": "null"}
```

---

## 🧪 Advanced Testing with Postman

### Import Collection

Create a Postman collection with these requests:

1. **Write-Through PUT**: `PUT http://localhost:8081/api/cache-patterns/write-through/{key}`
2. **Write-Through GET**: `GET http://localhost:8081/api/cache-patterns/write-through/{key}`
3. **Cache-Aside PUT**: `PUT http://localhost:8081/api/cache-patterns/cache-aside/{key}`
4. **Cache-Aside GET**: `GET http://localhost:8081/api/cache-patterns/cache-aside/{key}`
5. **Write-Around PUT**: `PUT http://localhost:8081/api/cache-patterns/write-around/{key}`
6. **Write-Around GET**: `GET http://localhost:8081/api/cache-patterns/write-around/{key}`
7. **Write-Back PUT**: `PUT http://localhost:8081/api/cache-patterns/write-back/{key}`
8. **Write-Back GET**: `GET http://localhost:8081/api/cache-patterns/write-back/{key}`
9. **Compare All**: `GET http://localhost:8081/api/cache-patterns/compare/{key}`

---

## 📊 Performance Monitoring

### Check Performance Logs

```bash
# View performance logs
cat cache-project/logs.txt

# Watch logs in real-time
tail -f cache-project/logs.txt
```

### Monitor Redis

```bash
# Connect to Redis CLI
redis-cli

# Check all keys
KEYS *

# Check specific cache
KEYS cacheEntries::*
KEYS cacheAside::*
KEYS writeAround::*
KEYS writeBack::*

# Check access counters (Write-Around)
KEYS access::*

# Get cache value
GET "cacheEntries::user1"

# Monitor Redis commands in real-time
MONITOR
```

### Monitor MySQL

```bash
# Connect to MySQL
mysql -u root -p

# Use database
USE cache_db;

# Check all entries
SELECT * FROM cache_entries;

# Check specific entry
SELECT * FROM cache_entries WHERE cache_key = 'user1';

# Count entries
SELECT COUNT(*) FROM cache_entries;
```

---

## 🎯 Expected Learning Outcomes

After running these tests, you should understand:

1. ✅ **Write-Through**: Fast reads, slower writes, high memory usage
2. ✅ **Cache-Aside**: Slow first read, memory efficient
3. ✅ **Write-Around**: Selective caching, prevents pollution
4. ✅ **Write-Back**: Fastest writes, eventual consistency

---

## 🐛 Troubleshooting

### Application won't start
```bash
# Check if ports are in use
netstat -ano | findstr :8081
netstat -ano | findstr :3306
netstat -ano | findstr :6379

# Kill process if needed
taskkill /PID <PID> /F
```

### Redis connection error
```bash
# Start Redis
redis-server

# Or on Windows with WSL
wsl redis-server
```

### MySQL connection error
```bash
# Check MySQL status
mysql -u root -p -e "SELECT 1"

# Create database if missing
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cache_db"
```

### Cache not working
```bash
# Clear all Redis caches
redis-cli FLUSHALL

# Restart application
mvn spring-boot:run
```

---

## 📚 Next Steps

1. ✅ Run all test scenarios
2. ✅ Compare performance results
3. ✅ Check Swagger UI: http://localhost:8081/swagger-ui.html
4. ✅ Read detailed guide: [`CACHE_PATTERNS_GUIDE.md`](CACHE_PATTERNS_GUIDE.md)
5. ✅ Experiment with your own use cases

---

**Happy Testing! 🚀**