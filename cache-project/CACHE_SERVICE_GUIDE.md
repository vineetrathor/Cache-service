# Cache Service - Complete Guide

## 📚 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [How to Test the APIs](#how-to-test-the-apis)
3. [Request Timing Feature](#request-timing-feature)
4. [Database Schema](#database-schema)
5. [Technology Stack](#technology-stack)
6. [Flow Explanation](#flow-explanation)

---

## 🏗️ Architecture Overview

This cache service follows a **3-Layer Architecture** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT (Browser/Postman)              │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP Request
                      ▼
┌─────────────────────────────────────────────────────────┐
│              CONTROLLER LAYER (CacheController)          │
│  - Handles HTTP requests (GET, PUT, DELETE)             │
│  - Validates input                                       │
│  - Returns HTTP responses                                │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│               SERVICE LAYER (CacheService)               │
│  - Contains business logic                               │
│  - Manages cache operations (put, get, delete)          │
│  - Handles cache hit/miss logic                         │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│            REPOSITORY LAYER (CacheRepository)            │
│  - Interfaces with database                              │
│  - Provides CRUD operations                              │
│  - Spring Data JPA auto-implements queries              │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│                  DATABASE (MySQL)                        │
│  - Stores cache entries persistently                     │
│  - Table: cache_entries                                  │
└─────────────────────────────────────────────────────────┘
```

### Additional Components:

**Interceptor (RequestTimingInterceptor)**
- Intercepts all HTTP requests
- Measures execution time
- Logs performance metrics
- Adds `X-Response-Time` header to responses

**Configuration Classes**
- `OpenApiConfig`: Configures Swagger documentation
- `WebConfig`: Registers interceptors and MVC settings

---

## 🧪 How to Test the APIs

### Method 1: Using Swagger UI (Recommended)

1. **Open Swagger UI**: http://localhost:8080/swagger-ui.html

2. **Test PUT endpoint** (Store data):
   - Click on `PUT /api/cache/{key}`
   - Click "Try it out"
   - Enter key: `user123`
   - Enter request body:
   ```json
   {
     "value": "John Doe"
   }
   ```
   - Click "Execute"
   - You should see a 200 response with the saved cache entry

3. **Test GET endpoint** (Retrieve data):
   - Click on `GET /api/cache/{key}`
   - Click "Try it out"
   - Enter key: `user123`
   - Click "Execute"
   - You should see the cached value

4. **Test GET ALL endpoint**:
   - Click on `GET /api/cache`
   - Click "Try it out"
   - Click "Execute"
   - You'll see all cache entries

5. **Test DELETE endpoint**:
   - Click on `DELETE /api/cache/{key}`
   - Click "Try it out"
   - Enter key: `user123`
   - Click "Execute"

### Method 2: Using cURL (Command Line)

```bash
# 1. Store a cache entry
curl -X PUT http://localhost:8080/api/cache/user123 \
  -H "Content-Type: application/json" \
  -d '{"value":"John Doe"}'

# 2. Retrieve the cached value
curl http://localhost:8080/api/cache/user123

# 3. Get all cache entries
curl http://localhost:8080/api/cache

# 4. Delete a cache entry
curl -X DELETE http://localhost:8080/api/cache/user123

# 5. Health check
curl http://localhost:8080/api/cache/health
```

### Method 3: Using Postman

1. Create a new request
2. Set method to PUT
3. URL: `http://localhost:8080/api/cache/user123`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
```json
{
  "value": "John Doe"
}
```
6. Send the request

---

## ⏱️ Request Timing Feature

### How It Works

The `RequestTimingInterceptor` automatically measures the execution time of every API request:

1. **Before Request**: Records start time
2. **After Request**: Calculates duration
3. **Logs Performance**: 
   - ✅ Fast requests (< 500ms): Green log
   - ⏱️ Normal requests (500-1000ms): Info log
   - ⚠️ Slow requests (> 1000ms): Warning log

### Where to See Timing

**1. In Console Logs:**
```
✅ Request Completed: GET /api/cache/user123 - Status: 200 - Duration: 45ms
```

**2. In HTTP Response Headers:**
Check the `X-Response-Time` header in the response:
```
X-Response-Time: 45ms
```

**3. In Swagger UI:**
After executing a request, check the response headers section.

### Example Log Output:
```
⏱️  Request Started: PUT /api/cache/user123
PUT operation - Key: user123, Value: John Doe
Creating new cache entry for key: user123
✅ Request Completed: PUT /api/cache/user123 - Status: 200 - Duration: 67ms
```

---

## 🗄️ Database Schema

### Table: `cache_entries`

| Column      | Type         | Description                    |
|-------------|--------------|--------------------------------|
| id          | BIGINT       | Primary key (auto-increment)   |
| cache_key   | VARCHAR(255) | Unique cache key               |
| cache_value | TEXT         | Cached value                   |
| created_at  | DATETIME     | Entry creation timestamp       |
| updated_at  | DATETIME     | Last update timestamp          |

### View Data in MySQL Workbench:

```sql
USE cache_db;
SELECT * FROM cache_entries;
```

---

## 🛠️ Technology Stack

| Technology | Purpose |
|------------|---------|
| **Spring Boot 3.5.14** | Application framework |
| **Spring Data JPA** | Database abstraction layer |
| **MySQL 8.4** | Persistent storage |
| **Lombok** | Reduce boilerplate code |
| **SpringDoc OpenAPI** | API documentation (Swagger) |
| **Spring DevTools** | Hot reload during development |
| **Maven** | Build and dependency management |

---

## 🔄 Flow Explanation

### PUT Request Flow (Storing Data)

```
1. Client sends PUT request
   ↓
2. RequestTimingInterceptor.preHandle() - Records start time
   ↓
3. CacheController.putCache() - Receives request
   ↓
4. Validates request body (checks if value exists)
   ↓
5. CacheService.put() - Business logic
   ↓
6. Checks if key already exists in database
   ↓
7. If exists: Updates existing entry
   If not: Creates new entry
   ↓
8. CacheRepository.save() - Saves to MySQL
   ↓
9. Returns saved CacheEntry
   ↓
10. RequestTimingInterceptor.afterCompletion() - Logs duration
   ↓
11. Response sent to client with timing header
```

### GET Request Flow (Retrieving Data)

```
1. Client sends GET request
   ↓
2. RequestTimingInterceptor.preHandle() - Records start time
   ↓
3. CacheController.getCache() - Receives request
   ↓
4. CacheService.get() - Business logic
   ↓
5. CacheRepository.findByKey() - Queries MySQL
   ↓
6. If found: Returns value (Cache HIT)
   If not found: Returns empty (Cache MISS)
   ↓
7. Controller formats response
   ↓
8. RequestTimingInterceptor.afterCompletion() - Logs duration
   ↓
9. Response sent to client
```

### Key Concepts:

**Cache HIT**: When requested data is found in cache
- Fast response
- No need to fetch from original source

**Cache MISS**: When requested data is not in cache
- Returns 404
- In a real system, would fetch from original source and cache it

**Automatic Timestamps**: 
- `@PrePersist`: Sets createdAt and updatedAt on first save
- `@PreUpdate`: Updates updatedAt on subsequent saves

---

## 📊 Performance Monitoring

### Metrics You Can Track:

1. **Response Time**: Check logs or `X-Response-Time` header
2. **Cache Hit Rate**: Count successful GET requests vs 404s
3. **Database Queries**: Check console (SQL queries are logged)
4. **Request Volume**: Count total requests in logs

### Example Performance Analysis:

```
✅ Request Completed: GET /api/cache/user123 - Status: 200 - Duration: 12ms  (Cache HIT)
✅ Request Completed: GET /api/cache/user456 - Status: 404 - Duration: 8ms   (Cache MISS)
✅ Request Completed: PUT /api/cache/user789 - Status: 200 - Duration: 45ms  (Write)
```

---

## 🚀 Running the Application

### Option 1: Maven Command
```bash
cd cache-project
mvn spring-boot:run
```

### Option 2: Maven Wrapper
```bash
cd cache-project
.\mvnw.cmd spring-boot:run
```

### Option 3: From IDE
Right-click `CacheProjectApplication.java` → Run Java

---

## 📝 API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/cache/{key}` | Store/update cache entry |
| GET | `/api/cache/{key}` | Retrieve cache entry |
| GET | `/api/cache` | Get all cache entries |
| DELETE | `/api/cache/{key}` | Delete cache entry |
| DELETE | `/api/cache` | Clear all cache |
| GET | `/api/cache/health` | Health check |

---

## 🎓 Learning Points

1. **Layered Architecture**: Separation of concerns (Controller → Service → Repository)
2. **Dependency Injection**: Spring automatically injects dependencies
3. **JPA Annotations**: `@Entity`, `@Table`, `@Column` map Java objects to database
4. **REST Principles**: HTTP methods (GET, PUT, DELETE) for CRUD operations
5. **Interceptors**: Cross-cutting concerns (logging, timing) without modifying business logic
6. **Lombok**: Reduces boilerplate with annotations (`@Data`, `@Slf4j`, etc.)
7. **Spring Data JPA**: Auto-implements repository methods based on naming conventions
8. **API Documentation**: Swagger provides interactive API testing interface

---

## 🔧 Next Steps for Enhancement

1. Add caching layer (Redis) for faster reads
2. Implement TTL (Time To Live) for cache expiration
3. Add authentication/authorization
4. Implement rate limiting
5. Add metrics dashboard (Actuator + Prometheus)
6. Add unit and integration tests
7. Implement cache eviction policies (LRU, LFU)
8. Add distributed caching support

---

**Happy Caching! 🚀**