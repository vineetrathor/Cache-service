# 🚀 Cache Service - Multi-Pattern Implementation

A comprehensive **Spring Boot cache service** implementing **4 fundamental caching patterns** with MySQL and Redis. Built as a learning project to understand different caching strategies and their trade-offs.

---

## 📚 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Cache Patterns Implemented](#cache-patterns-implemented)
- [Quick Start](#quick-start)
- [API Endpoints](#api-endpoints)
- [Documentation](#documentation)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Learning Outcomes](#learning-outcomes)

---

## 🎯 Overview

This project demonstrates **4 different caching patterns** used in production systems:

| Pattern | Write Speed | Read Speed | Memory Usage | Best For |
|---------|------------|------------|--------------|----------|
| **Write-Through** | Medium | ⚡ Fast | High | Read-heavy workloads |
| **Cache-Aside** | Fast | Medium | 💚 Low | Unpredictable access |
| **Write-Around** | Fast | Medium | 💚 Low | Write-heavy workloads |
| **Write-Back** | ⚡ Fastest | ⚡ Fast | Medium | High throughput |

Each pattern is implemented as a **separate service** with dedicated REST endpoints, allowing side-by-side comparison.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│  CachePatternController - Unified endpoint for all patterns │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐
│ Write-Through  │  │  Cache-Aside    │  │  Write-Around   │
│    Service     │  │    Service      │  │    Service      │
└───────┬────────┘  └────────┬────────┘  └────────┬────────┘
        │                     │                     │
        │           ┌─────────▼─────────┐          │
        │           │   Write-Back      │          │
        │           │     Service       │          │
        │           └─────────┬─────────┘          │
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐
│  Redis Cache   │  │  MySQL Database │  │  Performance    │
│  (In-Memory)   │  │  (Persistent)   │  │    Logging      │
└────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## 🎨 Cache Patterns Implemented

### 1️⃣ Write-Through Pattern
**Strategy**: Write to cache AND database simultaneously

```java
PUT → [MySQL + Redis] → Response (50-100ms)
GET → [Redis] → Hit? Return (5ms) : [MySQL] → Cache → Return
```

**Use Cases**: User profiles, product catalogs, frequently-read data

---

### 2️⃣ Cache-Aside Pattern (Lazy Loading)
**Strategy**: Write to database only, cache on read

```java
PUT → [MySQL only] → Response (30-50ms)
GET → [Redis] → Hit? Return (5ms) : [MySQL] → Cache → Return (50ms)
```

**Use Cases**: Large datasets, unpredictable access patterns, analytics

---

### 3️⃣ Write-Around Pattern
**Strategy**: Bypass cache on write, selective caching on read

```java
PUT → [MySQL only] → Invalidate cache → Response (30-50ms)
GET → [Redis] → Hit? Return : [MySQL] → Cache if hot (2+ accesses)
```

**Use Cases**: Write-heavy workloads, social media posts, news articles

---

### 4️⃣ Write-Back Pattern (Write-Behind)
**Strategy**: Write to cache immediately, async batch to database

```java
PUT → [Redis] → Queue → Response (5-10ms) ⚡ FASTEST
Background: Batch write queue to MySQL every 30s
GET → [Redis] → Hit? Return (5ms) : [MySQL] → Cache → Return
```

**Use Cases**: High-throughput writes, logging, metrics, counters

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+** (running on port 3306)
- **Redis 6.0+** (running on port 6379)

### Installation

1. **Clone the repository**
   ```bash
   cd cache-project
   ```

2. **Configure database** (if needed)
   ```properties
   # src/main/resources/application.properties
   spring.datasource.url=jdbc:mysql://localhost:3306/cache_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Start MySQL**
   ```bash
   mysql -u root -p
   CREATE DATABASE cache_db;
   ```

4. **Start Redis**
   ```bash
   redis-server
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Verify it's running**
   ```bash
   curl http://localhost:8081/api/cache-patterns/health
   ```

---

## 🌐 API Endpoints

### Base URL
```
http://localhost:8081/api/cache-patterns
```

### Write-Through Pattern
```bash
PUT    /write-through/{key}    # Store data (MySQL + Redis)
GET    /write-through/{key}    # Retrieve data
DELETE /write-through/{key}    # Delete data
```

### Cache-Aside Pattern
```bash
PUT    /cache-aside/{key}      # Store data (MySQL only)
GET    /cache-aside/{key}      # Retrieve data (lazy load)
DELETE /cache-aside/{key}      # Delete data
```

### Write-Around Pattern
```bash
PUT    /write-around/{key}     # Store data (bypass cache)
GET    /write-around/{key}     # Retrieve data (selective cache)
DELETE /write-around/{key}     # Delete data
```

### Write-Back Pattern
```bash
PUT    /write-back/{key}       # Store data (Redis + queue)
GET    /write-back/{key}       # Retrieve data
DELETE /write-back/{key}       # Delete data
```

### Comparison Endpoint
```bash
GET    /compare/{key}          # Compare all patterns
```

### Example Usage

```bash
# Store data in Write-Through pattern
curl -X PUT http://localhost:8081/api/cache-patterns/write-through/user123 \
  -H "Content-Type: text/plain" \
  -d "John Doe"

# Retrieve data
curl http://localhost:8081/api/cache-patterns/write-through/user123

# Compare all patterns
curl http://localhost:8081/api/cache-patterns/compare/user123
```

---

## 📖 Documentation

### Comprehensive Guides

1. **[CACHE_PATTERNS_GUIDE.md](CACHE_PATTERNS_GUIDE.md)** - Detailed explanation of all 4 patterns
   - Flow diagrams
   - Advantages/disadvantages
   - Performance characteristics
   - Code examples
   - Use cases

2. **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Ready-to-use test scenarios
   - Write performance comparison
   - Read performance comparison
   - Selective caching tests
   - Async behavior tests
   - Monitoring commands

3. **[REDIS_CACHE_GUIDE.md](REDIS_CACHE_GUIDE.md)** - Redis integration details
   - Configuration
   - Serialization
   - TTL management

4. **[CACHING_STRATEGIES_EXPLAINED.md](CACHING_STRATEGIES_EXPLAINED.md)** - Theory and concepts
   - When to use each pattern
   - Trade-offs
   - Real-world examples

### API Documentation

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs

---

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.4.1** - Application framework
- **Spring Data JPA** - Database abstraction
- **Spring Cache** - Caching abstraction
- **Spring Async** - Asynchronous processing

### Database & Cache
- **MySQL 8.0+** - Persistent storage
- **Redis 6.0+** - In-memory cache

### Documentation
- **SpringDoc OpenAPI 3** - API documentation
- **Swagger UI** - Interactive API explorer

### Build & Dependencies
- **Maven** - Dependency management
- **Lombok** - Boilerplate reduction

---

## 📁 Project Structure

```
cache-project/
├── src/main/java/com/vineet/cache_project/
│   ├── CacheProjectApplication.java       # Main application
│   ├── controller/
│   │   ├── CacheController.java           # Basic cache controller
│   │   ├── RedisCacheController.java      # Redis-specific controller
│   │   └── CachePatternController.java    # ⭐ Unified patterns controller
│   ├── service/
│   │   ├── CacheService.java              # Basic cache service
│   │   ├── WriteThroughCacheService.java  # Write-Through pattern
│   │   ├── CacheAsideService.java         # Cache-Aside pattern
│   │   ├── WriteAroundService.java        # Write-Around pattern
│   │   └── WriteBackService.java          # Write-Back pattern
│   ├── entity/
│   │   └── CacheEntry.java                # JPA entity
│   ├── repository/
│   │   └── CacheRepository.java           # JPA repository
│   ├── config/
│   │   ├── RedisConfig.java               # Redis configuration
│   │   ├── OpenApiConfig.java             # Swagger configuration
│   │   └── WebConfig.java                 # Web configuration
│   └── interceptor/
│       └── RequestTimingInterceptor.java  # Performance logging
├── src/main/resources/
│   └── application.properties             # Application config
├── CACHE_PATTERNS_GUIDE.md                # ⭐ Main patterns guide
├── TESTING_GUIDE.md                       # ⭐ Testing scenarios
├── REDIS_CACHE_GUIDE.md                   # Redis integration
├── CACHING_STRATEGIES_EXPLAINED.md        # Theory & concepts
├── PERFORMANCE_LOGS_README.md             # Performance monitoring
├── BUG_FIX_OPTIONAL_SERIALIZATION.md      # Bug fix documentation
└── logs.txt                               # Performance logs
```

---

## 🎓 Learning Outcomes

After working with this project, you will understand:

### ✅ Caching Fundamentals
- Cache hit vs cache miss
- Cache invalidation strategies
- TTL (Time To Live) management
- Cache consistency vs performance trade-offs

### ✅ Pattern Selection
- When to use Write-Through (read-heavy)
- When to use Cache-Aside (memory-efficient)
- When to use Write-Around (write-heavy)
- When to use Write-Back (high throughput)

### ✅ Spring Boot Features
- `@Cacheable` - Automatic caching on read
- `@CachePut` - Automatic caching on write
- `@CacheEvict` - Cache invalidation
- `@Async` - Asynchronous processing
- `@Scheduled` - Scheduled tasks

### ✅ Redis Integration
- RedisTemplate for manual cache operations
- Spring Cache abstraction
- Serialization configuration
- Access counter management

### ✅ Performance Optimization
- Measuring cache performance
- Identifying bottlenecks
- Batch processing for writes
- Selective caching strategies

---

## 📊 Performance Comparison

| Scenario | Write-Through | Cache-Aside | Write-Around | Write-Back |
|----------|--------------|-------------|--------------|------------|
| **Write Latency** | 50-100ms | 30-50ms | 30-50ms | ⚡ 5-10ms |
| **First Read** | ⚡ 5-10ms | 50-100ms | 50-100ms | ⚡ 5-10ms |
| **Subsequent Reads** | 5-10ms | 5-10ms | 5-10ms | 5-10ms |
| **Memory Usage** | High | 💚 Low | 💚 Low | Medium |
| **Consistency** | ✅ Strong | ⚠️ Eventual | ⚠️ Eventual | ⚠️ Eventual |
| **Data Loss Risk** | ❌ None | ❌ None | ❌ None | ⚠️ Possible |

---

## 🧪 Testing

### Run All Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify
```

### Manual Testing

Follow the comprehensive testing guide: **[TESTING_GUIDE.md](TESTING_GUIDE.md)**

Key test scenarios:
1. Write performance comparison
2. First read performance
3. Subsequent read performance
4. Write-Around selective caching
5. Write-Back async behavior
6. Cache pollution prevention
7. Pattern comparison endpoint

---

## 🔍 Monitoring

### Performance Logs

```bash
# View logs
cat logs.txt

# Watch in real-time
tail -f logs.txt
```

### Redis Monitoring

```bash
# Connect to Redis
redis-cli

# View all keys
KEYS *

# Monitor commands
MONITOR

# Check memory usage
INFO memory
```

### MySQL Monitoring

```bash
# Connect to MySQL
mysql -u root -p cache_db

# View all entries
SELECT * FROM cache_entries;

# Count entries
SELECT COUNT(*) FROM cache_entries;
```

---

## 🐛 Troubleshooting

### Common Issues

1. **Port already in use**
   ```bash
   # Check ports
   netstat -ano | findstr :8081
   netstat -ano | findstr :3306
   netstat -ano | findstr :6379
   ```

2. **Redis connection error**
   ```bash
   # Start Redis
   redis-server
   ```

3. **MySQL connection error**
   ```bash
   # Create database
   mysql -u root -p -e "CREATE DATABASE cache_db"
   ```

4. **Cache not working**
   ```bash
   # Clear Redis
   redis-cli FLUSHALL
   
   # Restart application
   mvn spring-boot:run
   ```

---

## 🚀 Next Steps

### Phase B: Advanced Features (Future)
- [ ] Cache eviction policies (LRU, LFU)
- [ ] Distributed caching with Redis Cluster
- [ ] Cache warming strategies
- [ ] Multi-level caching (L1 + L2)
- [ ] Cache metrics and monitoring dashboard
- [ ] Circuit breaker for cache failures

### Phase C: Production Readiness (Future)
- [ ] Docker containerization
- [ ] Kubernetes deployment
- [ ] Load testing with JMeter
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] Health checks and readiness probes

---

## 📝 License

This project is created for educational purposes.

---

## 👨‍💻 Author

**Vineet**

Built as a learning project to understand caching patterns in distributed systems.

---

## 🙏 Acknowledgments

- Spring Boot Documentation
- Redis Documentation
- Martin Fowler's articles on caching patterns
- System Design Interview resources

---

## 📞 Support

For questions or issues:
1. Check the documentation guides
2. Review the testing guide
3. Check troubleshooting section
4. Review Swagger UI for API details

---

**Happy Learning! 🎓**

*Remember: The best way to learn is by experimenting. Try different patterns, measure performance, and understand the trade-offs!*