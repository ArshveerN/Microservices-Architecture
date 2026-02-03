# 🛒 E-Commerce Microservices Architecture

A distributed **microservices-based e-commerce system** built with **Java** and **Python**. The system is composed of independently deployable services for **Users**, **Products**, and **Orders**, all communicating through a centralized **Inter-Service Communication Service (ISCS)** proxy using RESTful APIs.

This project demonstrates core backend engineering concepts including service isolation, REST design, inter-service communication, request validation, and basic security practices.

---

## ✨ Key Features

- 🔹 **Microservices Architecture** – Independent User, Product, and Order services
- 🔹 **Language Interoperability** – Java services + Python proxy/client
- 🔹 **Centralized Request Routing** – All service calls go through ISCS
- 🔹 **RESTful APIs** – JSON-based communication over HTTP
- 🔹 **Password Security** – SHA-256 hashing for user credentials
- 🔹 **Config-Driven Service Discovery** – No hardcoded ports or IPs
- 🔹 **Automated Workload Execution** – Scripted client requests

---

## 🧱 System Architecture

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
                    │  Routing & Validation   │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │  Workload Parser Client │
                    │        (Python)         │
                    └─────────────────────────┘
```

All external requests are sent to **ISCS**, which validates input and forwards requests to the appropriate backend service.

---

## 🧩 Services Overview

| Service | Language | Responsibility |
|-------|----------|----------------|
| **UserService** | Java | User CRUD operations and password hashing |
| **ProductService** | Java | Product inventory management |
| **OrderService** | Java | Order placement with user/product validation |
| **ISCS** | Python | Central request router and validator |
| **Workload Parser** | Python | Automated client for batch request execution |

---

## 🛠 Tech Stack

- **Backend Services:** Java (HttpServer API)
- **Proxy & Client:** Python (`http.server`, `requests`)
- **Communication:** RESTful APIs (JSON over HTTP)
- **Security:** SHA-256 password hashing
- **Configuration:** JSON-based service discovery
- **Build & Run:** Bash scripts

---

## 🚀 Quick Start

### 1️⃣ Compile All Services

```bash
chmod +x ./runme.sh
./runme.sh -c
```

### 2️⃣ Start Services

Run **each command in a separate terminal**:

```bash
./runme.sh -u    # Start User Service
./runme.sh -p    # Start Product Service
./runme.sh -o    # Start Order Service
./runme.sh -i    # Start ISCS Proxy
```

### 3️⃣ Run a Workload File

```bash
./runme.sh -w <workload_file>
```

The workload parser sends a sequence of HTTP requests through ISCS and prints the responses.

---

## 🔌 API Reference

### 👤 User Service (`/user`)

| Method | Endpoint | Description |
|------|---------|-------------|
| GET | `/user/{id}` | Retrieve user by ID |
| POST | `/user` | Create, update, or delete a user |

**POST Body:**
```json
{
  "command": "create | update | delete",
  "id": 123,
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

---

### 📦 Product Service (`/product`)

| Method | Endpoint | Description |
|------|---------|-------------|
| GET | `/product/{id}` | Retrieve product by ID |
| POST | `/product` | Create, update, or delete a product |

**POST Body:**
```json
{
  "command": "create | update | delete",
  "id": 456,
  "name": "Widget",
  "description": "A useful widget",
  "price": 19.99,
  "quantity": 100
}
```

---

### 🧾 Order Service (`/order`)

| Method | Endpoint | Description |
|------|---------|-------------|
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

The order service validates:
- User existence
- Product existence
- Available inventory

---

## ⚙️ Configuration

Service locations are defined in `config.json`:

```json
{
  "UserService": { "ip": "127.0.0.1", "port": 14001 },
  "ProductService": { "ip": "127.0.0.1", "port": 15000 },
  "OrderService": { "ip": "127.0.0.1", "port": 14000 },
  "InterServiceCommunication": { "ip": "127.0.0.1", "port": 14002 }
}
```

This allows services to be moved or scaled without code changes.

---

## 📁 Project Structure

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
└── docs/               # Generated Javadocs
```

---

## 📡 HTTP Status Codes

| Code | Meaning |
|----|--------|
| **200** | Request successful |
| **400** | Invalid request or missing fields |
| **404** | Resource not found |
| **409** | Conflict (e.g., duplicate ID) |

---

## 🔮 Future Improvements

- JWT-based authentication
- Persistent storage (database integration)
- Service replication and load balancing
- Docker / Docker Compose deployment
- Circuit breaker and retry logic

---

## 📚 Documentation

- Javadocs are available in the `docs/` directory
- Inline comments explain request flow and validation logic

---

**Built to showcase backend system design, service communication, and clean API structure.**

