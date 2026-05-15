# 🚀 Cache Service - Learning Distributed Caching

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue?style=for-the-badge&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-6.0+-red?style=for-the-badge&logo=redis)
![Maven](https://img.shields.io/badge/Maven-3.6+-purple?style=for-the-badge&logo=apache-maven)

**A comprehensive learning project implementing 4 fundamental caching patterns in distributed systems**

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Project Evolution](#-project-evolution)

</div>

---

## 📖 About

This project is a **hands-on learning journey** into caching strategies and distributed systems. It implements **4 production-grade caching patterns** with Spring Boot, MySQL, and Redis, demonstrating real-world trade-offs between performance, consistency, and memory usage.

### 🎯 Learning Objectives

- ✅ Understand different caching patterns and when to use them
- ✅ Master Spring Cache abstraction (@Cacheable, @CachePut, @CacheEvict)
- ✅ Learn Redis integration and manual cache management
- ✅ Implement async processing and batch operations
- ✅ Measure and optimize cache performance
- ✅ Handle cache consistency and invalidation

---

## ✨ Features

### 🎨 4 Cache Patterns Implemented

<table>
<tr>
<td width="50%">

#### 1️⃣ Write-Through Pattern
```
PUT → MySQL + Redis (immediate)
GET → Redis → MySQL (if miss)
```
**Best for:** Read-heavy workloads
- ⚡ Fast first reads (5-10ms)
- 🔄 Strong consistency
- 📊 Higher memory usage

</td>
<td width="50%">

#### 2️⃣ Cache-Aside Pattern
```
PUT → MySQL only (no caching)
GET → Redis → MySQL → Cache
```
**Best for:** Unpredictable access
- 💚 Memory efficient
- 🔄 Lazy loading
- 📊 Slower first read (50-100ms)

</td>
</tr>
<tr>
<td width="50%">

#### 3️⃣ Write-Around Pattern
```
PUT → MySQL (bypass cache)
GET → Selective caching (2+ accesses)
```
**Best for:** Write-heavy workloads
- 🎯 Prevents cache pollution
- 💚 Optimal memory usage
- 📊 Adaptive caching

</td>
<td width="50%">

#### 4️⃣ Write-Back Pattern
```
PUT → Redis + Queue (async)
Background → Batch to MySQL (30s)
```
**Best for:** High throughput
- ⚡ Fastest writes (5-10ms)
- 🚀 Highest throughput
- ⚠️ Eventual consistency

</td>
</tr>
</table>

### 🌐 REST API

- **16 endpoints** across 4 patterns (PUT/GET/DELETE for each)
- **Comparison endpoint** to test all patterns side-by-side
- **Full Swagger documentation** with interactive UI
- **Performance monitoring** with request timing logs

### 📊 Performance Monitoring

- Real-time performance logging to CSV
- Request timing interceptor
- Redis command monitoring
- MySQL query tracking

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  REST API Layer                          │
│         CachePatternController (Unified)                 │
└─────────────────────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌──────▼──────┐  ┌─────▼───────┐
│Write-Through │  │Cache-Aside  │  │Write-Around │
│   Service    │  │  Service    │  │  Service    │
└───────┬──────┘  └──────┬──────┘  └─────┬───────┘
        │                │                │
        │      ┌─────────▼─────────┐     │
        │      │   Write-Back      │     │
        │      │    Service        │     │
        │      └─────────┬─────────┘     │
        │                │                │
        └────────────────┼────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌──────▼──────┐  ┌─────▼───────┐
│    Redis     │  │    MySQL    │  │ Performance │
│  (Cache)     │  │ (Database)  │  │   Logs      │
└──────────────┘  └─────────────┘  └─────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

```bash
☑️ Java 17+
☑️ Maven 3.6+
☑️ MySQL 8.0+ (port 3306)
☑️ Redis 6.0+ (port 6379)
```

### Installation

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd Cache-service/cache-project

# 2. Start MySQL
mysql -u root -p
CREATE DATABASE cache_db;

# 3. Start Redis
redis-server

# 4. Run the application
mvn spring-boot:run

# 5. Verify it's running
curl http://localhost:8081/api/cache-patterns/health
```

### Quick Test

```bash
# Store data in Write-Through pattern
curl -X PUT http://localhost:8081/api/cache-patterns/write-through/user1 \
  -H "Content-Type: text/plain" \
  -d "Alice"

# Retrieve data
curl http://localhost:8081/api/cache-patterns/write-through/user1

# Compare all patterns
curl http://localhost:8081/api/cache-patterns/compare/user1
```

---

## 📚 Documentation

### 📖 Comprehensive Guides

| Document | Description |
|----------|-------------|
| **[cache-project/README.md](cache-project/README.md)** | Complete project documentation |
| **[CACHE_PATTERNS_GUIDE.md](cache-project/CACHE_PATTERNS_GUIDE.md)** | Detailed pattern explanations with examples |
| **[TESTING_GUIDE.md](cache-project/TESTING_GUIDE.md)** | Ready-to-use test scenarios |
| **[REDIS_CACHE_GUIDE.md](cache-project/REDIS_CACHE_GUIDE.md)** | Redis integration details |
| **[CACHING_STRATEGIES_EXPLAINED.md](cache-project/CACHING_STRATEGIES_EXPLAINED.md)** | Theory and concepts |

### 🌐 API Documentation

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs

---

## 📈 Project Evolution

### 🎯 Phase 0: Foundation (Initial Commit)

<details>
<summary><b>✅ Basic Cache Service with MySQL</b></summary>

#### Features Added
- ✅ Spring Boot project setup with Maven
- ✅ MySQL database integration
- ✅ JPA entity and repository
- ✅ Basic REST controller (GET/PUT/DELETE)
- ✅ Application configuration

#### Files Created
```
CacheProjectApplication.java    # Main application
CacheEntry.java                 # JPA entity
CacheRepository.java            # JPA repository
CacheController.java            # REST controller
CacheService.java               # Business logic
application.properties          # Configuration
```

#### What You Learned
- Spring Boot project structure
- JPA/Hibernate basics
- REST API design
- MySQL integration

</details>

---

### 🎯 Phase 1: Redis Integration

<details>
<summary><b>✅ Two-Tier Caching with Redis</b></summary>

#### Features Added
- ✅ Redis cache integration
- ✅ Spring Cache abstraction
- ✅ Write-Through caching pattern
- ✅ Performance monitoring
- ✅ Swagger/OpenAPI documentation

#### Files Created/Modified
```
RedisConfig.java                # Redis configuration
RedisCacheService.java          # Redis cache service
RedisCacheController.java       # Redis endpoints
OpenApiConfig.java              # Swagger config
WebConfig.java                  # Web configuration
RequestTimingInterceptor.java  # Performance logging
```

#### Documentation Added
```
REDIS_CACHE_GUIDE.md           # Redis integration guide
CACHE_SERVICE_GUIDE.md         # Basic cache guide
PERFORMANCE_LOGS_README.md     # Performance monitoring
```

#### What You Learned
- Redis integration with Spring Boot
- Spring Cache annotations (@Cacheable, @CachePut)
- Performance monitoring
- API documentation with Swagger

</details>

---

### 🎯 Phase 2: Bug Fixes & Optimization

<details>
<summary><b>✅ Serialization Issues & Type Consistency</b></summary>

#### Issues Fixed
- 🐛 ClassCastException: LinkedHashMap cannot be cast to String
- 🐛 Optional<String> serialization problems
- 🐛 Cache type inconsistency

#### Solution Implemented
- ✅ All cache methods return `String` type consistently
- ✅ Removed Optional<String> from cached methods
- ✅ Updated serialization configuration

#### Documentation Added
```
BUG_FIX_OPTIONAL_SERIALIZATION.md  # Bug fix documentation
```

#### What You Learned
- Redis serialization challenges
- Type consistency in caching
- Debugging cache issues

</details>

---

### 🎯 Phase 3: Multiple Cache Patterns

<details>
<summary><b>✅ Implemented 4 Fundamental Caching Patterns</b></summary>

#### Patterns Implemented

**1. Write-Through Pattern**
```java
WriteThroughCacheService.java
- Immediate caching on write
- Fast first reads
- Strong consistency
```

**2. Cache-Aside Pattern**
```java
CacheAsideService.java
- Lazy loading
- Memory efficient
- No caching on write
```

**3. Write-Around Pattern**
```java
WriteAroundService.java
- Selective caching
- Access tracking
- Prevents cache pollution
```

**4. Write-Back Pattern**
```java
WriteBackService.java
- Async batch writes
- Highest throughput
- Eventual consistency
```

#### Configuration Updates
```
CacheProjectApplication.java   # Added @EnableAsync, @EnableScheduling
RedisConfig.java               # Multi-cache configuration
```

#### What You Learned
- Different caching strategies
- Async processing with @Async
- Scheduled tasks with @Scheduled
- Manual cache management with RedisTemplate
- Access pattern tracking

</details>

---

### 🎯 Phase 4: Unified API & Documentation

<details>
<summary><b>✅ Complete REST API & Comprehensive Documentation</b></summary>

#### Features Added
- ✅ Unified controller for all patterns
- ✅ Comparison endpoint
- ✅ Enhanced Swagger documentation
- ✅ Comprehensive testing guide
- ✅ Pattern selection guide

#### Files Created
```
CachePatternController.java    # Unified REST controller (16 endpoints)
CACHE_PATTERNS_GUIDE.md        # Complete pattern guide (638 lines)
TESTING_GUIDE.md               # Testing scenarios (382 lines)
CACHING_STRATEGIES_EXPLAINED.md # Theory and concepts
```

#### API Endpoints
```
/api/cache-patterns/write-through/{key}   - PUT/GET/DELETE
/api/cache-patterns/cache-aside/{key}     - PUT/GET/DELETE
/api/cache-patterns/write-around/{key}    - PUT/GET/DELETE
/api/cache-patterns/write-back/{key}      - PUT/GET/DELETE
/api/cache-patterns/compare/{key}         - GET (compare all)
/api/cache-patterns/health                - GET (health check)
```

#### What You Learned
- REST API design patterns
- Comprehensive documentation
- Performance comparison
- Testing strategies

</details>

---

## 📊 Performance Comparison

| Metric | Write-Through | Cache-Aside | Write-Around | Write-Back |
|--------|--------------|-------------|--------------|------------|
| **Write Latency** | 50-100ms | 30-50ms | 30-50ms | ⚡ **5-10ms** |
| **First Read** | ⚡ **5-10ms** | 50-100ms | 50-100ms | ⚡ **5-10ms** |
| **Subsequent Reads** | 5-10ms | 5-10ms | 5-10ms | 5-10ms |
| **Memory Usage** | High | 💚 **Low** | 💚 **Low** | Medium |
| **Consistency** | ✅ Strong | ⚠️ Eventual | ⚠️ Eventual | ⚠️ Eventual |
| **Data Loss Risk** | ❌ None | ❌ None | ❌ None | ⚠️ Possible |

---

## 🛠️ Technology Stack

<table>
<tr>
<td>

**Backend**
- Spring Boot 3.4.1
- Spring Data JPA
- Spring Cache
- Spring Async

</td>
<td>

**Database & Cache**
- MySQL 8.0+
- Redis 6.0+
- HikariCP

</td>
<td>

**Documentation**
- SpringDoc OpenAPI 3
- Swagger UI
- Markdown

</td>
<td>

**Build Tools**
- Maven
- Lombok

</td>
</tr>
</table>

---

## 🎓 What You'll Learn

### ✅ Caching Fundamentals
- Cache hit vs cache miss
- Cache invalidation strategies
- TTL (Time To Live) management
- Cache consistency patterns

### ✅ Spring Boot Features
- Spring Cache abstraction
- Async processing
- Scheduled tasks
- REST API design

### ✅ Redis Operations
- RedisTemplate usage
- Manual cache management
- Access counters
- Batch operations

### ✅ Performance Optimization
- Measuring cache performance
- Identifying bottlenecks
- Batch processing
- Selective caching

---

## 🧪 Testing

### Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Manual testing
# Follow TESTING_GUIDE.md for comprehensive test scenarios
```

### Test Scenarios

1. ✅ Write performance comparison
2. ✅ First read performance
3. ✅ Subsequent read performance
4. ✅ Write-Around selective caching
5. ✅ Write-Back async behavior
6. ✅ Cache pollution prevention
7. ✅ Pattern comparison

---

## 📁 Project Structure

```
Cache-service/
├── README.md                              # This file
└── cache-project/
    ├── src/main/java/com/vineet/cache_project/
    │   ├── CacheProjectApplication.java   # Main application
    │   ├── controller/
    │   │   ├── CacheController.java       # Basic controller
    │   │   ├── RedisCacheController.java  # Redis controller
    │   │   └── CachePatternController.java # ⭐ Unified controller
    │   ├── service/
    │   │   ├── CacheService.java
    │   │   ├── WriteThroughCacheService.java
    │   │   ├── CacheAsideService.java
    │   │   ├── WriteAroundService.java
    │   │   └── WriteBackService.java
    │   ├── entity/
    │   │   └── CacheEntry.java
    │   ├── repository/
    │   │   └── CacheRepository.java
    │   ├── config/
    │   │   ├── RedisConfig.java
    │   │   ├── OpenApiConfig.java
    │   │   └── WebConfig.java
    │   └── interceptor/
    │       └── RequestTimingInterceptor.java
    ├── src/main/resources/
    │   └── application.properties
    ├── README.md                          # Project documentation
    ├── CACHE_PATTERNS_GUIDE.md            # ⭐ Pattern guide
    ├── TESTING_GUIDE.md                   # ⭐ Testing guide
    ├── REDIS_CACHE_GUIDE.md
    ├── CACHING_STRATEGIES_EXPLAINED.md
    ├── PERFORMANCE_LOGS_README.md
    ├── BUG_FIX_OPTIONAL_SERIALIZATION.md
    └── logs.txt                           # Performance logs
```

---

## 🚀 Future Enhancements

### Phase B: Advanced Features
- [ ] Cache eviction policies (LRU, LFU, FIFO)
- [ ] Distributed caching with Redis Cluster
- [ ] Cache warming strategies
- [ ] Multi-level caching (L1 + L2)
- [ ] Metrics dashboard with Grafana
- [ ] Circuit breaker for cache failures

### Phase C: Production Readiness
- [ ] Docker containerization
- [ ] Kubernetes deployment
- [ ] Load testing with JMeter
- [ ] Prometheus metrics
- [ ] Health checks and readiness probes
- [ ] CI/CD pipeline

---

## 🤝 Contributing

This is a learning project, but contributions are welcome! Feel free to:

1. 🐛 Report bugs
2. 💡 Suggest new features
3. 📖 Improve documentation
4. 🧪 Add test cases

---

## 📝 License

This project is created for educational purposes.

---

## 👨‍💻 Author

**Vineet**

Built as a comprehensive learning project to understand caching patterns in distributed systems.

---

## 🙏 Acknowledgments

- Spring Boot Documentation
- Redis Documentation
- Martin Fowler's articles on caching patterns
- System Design Interview resources

---

## 📞 Support

### Documentation
- 📖 [Complete Guide](cache-project/CACHE_PATTERNS_GUIDE.md)
- 🧪 [Testing Guide](cache-project/TESTING_GUIDE.md)
- 🌐 [Swagger UI](http://localhost:8081/swagger-ui.html)

### Troubleshooting
- Check [TESTING_GUIDE.md](cache-project/TESTING_GUIDE.md) troubleshooting section
- Review application logs
- Verify MySQL and Redis are running

---

<div align="center">

### ⭐ Star this repo if you found it helpful!

**Happy Learning! 🎓**

*The best way to learn is by doing. Experiment with different patterns and understand the trade-offs!*

---

Made with ❤️ for learning distributed systems

</div>
