# 🏷️ CloudCart Product Service

The Product Catalog service for the CloudCart E-commerce platform. It manages product information, inventory levels, and categorization — with a Redis caching layer for high-performance reads.

## 🛠️ Tech Stack
*   **Framework:** Spring Boot 3.4
*   **Database:** PostgreSQL (uses `product_db`)
*   **Cache:** Redis 7 (via Spring Cache + `spring-boot-starter-data-redis`)
*   **ORM:** Spring Data JPA / Hibernate
*   **Security:** JWT Validation (Stateless), Role-based Access Control

## 🎯 Responsibilities
*   **Catalog Management:** Provides CRUD operations for products.
*   **Inventory Control:** Manages stock levels with atomic updates to prevent race conditions during concurrent checkouts.
*   **Read Caching:** Caches product reads in Redis to reduce DB load and improve response times.
*   **Cache Invalidation:** Automatically evicts stale cache entries whenever product data changes.
*   **Authorization Enforcement:** Read operations are public, but mutating operations (Create, Update, Delete) require the `ADMIN` role.

## 📡 API Endpoints

| Method | Endpoint | Auth Required | Cached | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/products` | No | ✅ Yes | List all products (supports `?category=` and `?search=`) |
| `GET` | `/products/{id}` | No | ✅ Yes | Retrieve a single product by ID |
| `POST` | `/products` | Yes (ADMIN) | 🗑️ Evicts all | Create a new product |
| `PUT` | `/products/{id}` | Yes (ADMIN) | 🗑️ Evicts entry + all | Update an existing product |
| `DELETE` | `/products/{id}` | Yes (ADMIN) | 🗑️ Evicts entry + all | Delete a product |
| `PATCH` | `/products/{id}/stock` | Yes (JWT) | 🗑️ Evicts entry + all | Atomically adjust stock |

## ⚡ Caching Architecture

### Cache Names
| Cache | Key Pattern | Content |
| :--- | :--- | :--- |
| `products` | `all` | Full product list |
| `products` | `category:<name>` | Products filtered by category |
| `products` | `search:<term>` | Products filtered by search term |
| `product` | `<id>` | Single product by ID |

### Cache TTL
All entries expire automatically after **5 minutes**.

### Cache Invalidation
Any write operation (POST, PUT, DELETE, PATCH stock) triggers `@CacheEvict` annotations in `ProductService` which immediately clear:
*   The specific `product:<id>` entry
*   All `products:*` list entries

This ensures the Admin Dashboard always reflects fresh, accurate data.

### Graceful Fallback
If Redis is unreachable at startup, `CacheConfig.java` catches the connection error and installs a `NoOpCacheManager`. The service continues serving all requests directly from PostgreSQL — **Redis being down will never crash the API.**

### Observability
Cache hits and misses are logged at `DEBUG` level:
```
Cache MISS product:5 — loading from DB
Cache EVICT product:5 + products:all — stock updated by -1
```

Enable with `logging.level.com.cloudcart.product.service: DEBUG` in your environment.

### Atomic Stock Updates
The `/stock` endpoint uses a custom JPQL `@Modifying` query to adjust stock directly in the database:
```sql
UPDATE products SET stock = stock + :quantity WHERE id = :id AND stock + :quantity >= 0
```
This ensures stock never drops below zero and prevents concurrent transaction issues. The cache entry is evicted **after** the DB update succeeds.

## 🏗️ Code Structure

```
product/
├── config/
│   └── CacheConfig.java        # Redis CacheManager bean + graceful fallback
├── controller/
│   └── ProductController.java  # Thin REST layer — delegates to service
├── service/
│   └── ProductService.java     # @Cacheable/@CacheEvict annotations live here
├── entity/
│   └── Product.java            # JPA entity (implements Serializable for Redis)
├── repository/
│   └── ProductRepository.java  # JPA + atomic stock JPQL query
└── security/
    ├── SecurityConfig.java     # Public GETs, ADMIN writes
    └── JwtAuthenticationFilter.java
```

## 🚀 Running Locally

### Prerequisites
*   Java 17+
*   Maven
*   PostgreSQL running locally (with `product_db` created)
*   Redis running locally on port `6379`

### Environment Variables

```bash
export JWT_SECRET=your-super-secret-key-minimum-32-chars
export REDIS_HOST=localhost
export REDIS_PORT=6379
mvn spring-boot:run
```

> **Tip:** If you don't have Redis locally, the service still works — it will log a warning and fall back to DB-only mode.

## 🐳 Docker

```bash
docker build -t cloudcart-product-service:v1.0.0 .
docker run -e JWT_SECRET=secret -e REDIS_HOST=host.docker.internal cloudcart-product-service:v1.0.0
```
