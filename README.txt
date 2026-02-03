# E-Commerce Microservices Architecture

A distributed microservices-based e-commerce system built with Java and Python. Features independent User, Product, and Order services communicating via RESTful APIs through a central inter-service communication proxy.

## Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  User Service   │     │ Product Service │     │  Order Service  │
│   (Java)        │     │   (Java)        │     │   (Java)        │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │   ISCS Proxy (Python)   │
                    │  Request Routing &      │
                    │  Validation             │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │   Workload Parser       │
                    │   (Python Client)       │
                    └─────────────────────────┘
```

## Services

| Service | Language | Description |
|---------|----------|-------------|
| **UserService** | Java | User management with CRUD operations and SHA-256 password hashing |
| **ProductService** | Java | Product inventory management (create, update, delete, info) |
| **OrderService** | Java | Order processing with user/product validation |
| **ISCS** | Python | Inter-service communication proxy for request routing |

## Tech Stack

- **Backend:** Java (HttpServer API)
- **Proxy:** Python (http.server, requests)
- **Communication:** RESTful APIs with JSON
- **Security:** SHA-256 password hashing
- **Configuration:** JSON-based service discovery

## Quick Start

### 1. Compile
```bash
chmod +x ./runme.sh
./runme.sh -c
```

### 2. Start Services

Run each command in a **separate terminal**:
```bash
./runme.sh -u    # User Service
./runme.sh -p    # Product Service
./runme.sh -i    # ISCS Proxy
./runme.sh -o    # Order Service
```

### 3. Run Workload
```bash
./runme.sh -w <workload_file>
```

## API Reference

### User Service `/user`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/user/{id}` | Retrieve user by ID |
| POST | `/user` | Create, update, or delete user |

**POST Body:**
```json
{
  "command": "create|update|delete",
  "id": 123,
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

### Product Service `/product`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/product/{id}` | Retrieve product by ID |
| POST | `/product` | Create, update, or delete product |

**POST Body:**
```json
{
  "command": "create|update|delete",
  "id": 456,
  "name": "Widget",
  "description": "A useful widget",
  "price": 19.99,
  "quantity": 100
}
```

### Order Service `/order`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/order` | Place an order |

**POST Body:**
```json
{
  "command": "place order",
  "user_id": 123,
  "product_id": 456,
  "quantity": 2
}
```

## Configuration

Services are configured via `config.json`:
```json
{
  "UserService": { "ip": "127.0.0.1", "port": 14001 },
  "ProductService": { "ip": "127.0.0.1", "port": 15000 },
  "OrderService": { "ip": "127.0.0.1", "port": 14000 },
  "InterServiceCommunication": { "ip": "127.0.0.1", "port": 14002 }
}
```

## Project Structure
```
├── config.json
├── runme.sh
├── src/
│   ├── UserService/
│   │   └── UserServer.java
│   ├── ProductService/
│   │   └── ProductServer.java
│   ├── OrderService/
│   │   └── OrderServer.java
│   ├── ISCS/
│   │   └── ISCS.py
│   └── WorkloadParser.py
└── docs/                # Javadocs
```

## Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad request / Invalid fields |
| 404 | Resource not found |
| 409 | Conflict (ID already exists) |
