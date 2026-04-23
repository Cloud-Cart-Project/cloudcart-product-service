# 🏷️ CloudCart Product Service

The Product Catalog service for the CloudCart E-commerce platform. It manages product information, inventory levels, and categorization.

## 🛠️ Tech Stack
*   **Framework:** Spring Boot 3.4
*   **Database:** PostgreSQL (uses `product_db`)
*   **ORM:** Spring Data JPA / Hibernate
*   **Security:** JWT Validation (Stateless), Role-based Access Control

## 🎯 Responsibilities
*   **Catalog Management:** Provides CRUD operations for products.
*   **Inventory Control:** Manages stock levels with atomic updates to prevent race conditions during concurrent checkouts.
*   **Authorization Enforcement:** Read operations are public, but mutating operations (Create, Update, Delete) require the `ADMIN` role.

## 📡 API Endpoints

| Method | Endpoint | Auth Required | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | No | List all products (supports `?category=` filtering) |
| `GET` | `/products/{id}` | No | Retrieve a single product by ID |
| `POST` | `/products` | Yes (ADMIN) | Create a new product |
| `PUT` | `/products/{id}` | Yes (ADMIN) | Update an existing product |
| `DELETE` | `/products/{id}` | Yes (ADMIN) | Delete a product |
| `PATCH` | `/products/{id}/stock` | Yes (JWT) | Atomically increment/decrement stock by a `quantity` |

### Atomic Stock Updates
The `/stock` endpoint uses a custom JPQL `@Modifying` query to adjust stock directly in the database:
```sql
UPDATE products SET stock = stock + :quantity WHERE id = :id AND stock + :quantity >= 0
```
This ensures stock never drops below zero and prevents concurrent transaction issues.

## 🚀 Running Locally

### Prerequisites
*   Java 17+
*   Maven
*   PostgreSQL running locally (with `product_db` created)

### Environment Variables

```bash
export JWT_SECRET=your-super-secret-key-minimum-32-chars
mvn spring-boot:run
```

## 🐳 Docker

```bash
docker build -t cloudcart-product-service:v1.0.0 .
```
