# OrgChart API

REST API for managing an employee org hierarchy. Supports full CRUD, manager assignment, cycle detection, and tree traversal.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 24 |
| Framework | Spring Boot 4.0.8 (Spring MVC) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Neon serverless) |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 3.0.0 |
| Build | Maven Wrapper |

## Data Model

`Employee` has a self-referencing relationship:

- `manager` — `@ManyToOne(LAZY)`, nullable (CEO has no manager)
- `reports` — `@OneToMany(mappedBy = "manager")`, lazy collection of direct reports

Hierarchy rules enforced at the service layer:
- An employee cannot be their own manager.
- Setting a manager that creates a cycle (A → B → C → A) is rejected with `400`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/employees` | Create employee — returns `201 Created` + `Location` header |
| `GET` | `/employees` | List all employees (paginated + sortable) |
| `GET` | `/employees/{id}` | Get employee by ID |
| `PUT` | `/employees/{id}` | Update employee |
| `DELETE` | `/employees/{id}` | Delete employee; direct reports reassigned to deleted employee's manager |
| `GET` | `/employees/{id}/reports` | Direct reports of employee |
| `GET` | `/employees/{id}/chain` | Reporting chain from employee up to CEO |
| `GET` | `/employees/{id}/tree` | Recursive nested org tree rooted at employee |

### Query parameters for `GET /employees`

```
?page=0&size=10&sort=name,asc
```

## Example: Build a Hierarchy

### 1. Create CEO

```bash
curl -s -X POST http://localhost:8080/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Chen","designation":"CEO","salary":200000}' | jq .
```

Note the returned `id` (e.g. `1`).

### 2. Create Manager under CEO

```bash
curl -s -X POST http://localhost:8080/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob Smith","designation":"Engineering Manager","salary":150000,"managerId":1}' | jq .
```

Note the returned `id` (e.g. `2`).

### 3. Create Engineer under Manager

```bash
curl -s -X POST http://localhost:8080/employees \
  -H "Content-Type: application/json" \
  -d '{"name":"Carol Jones","designation":"Software Engineer","salary":120000,"managerId":2}' | jq .
```

### 4. Fetch Org Tree from CEO

```bash
curl -s http://localhost:8080/employees/1/tree | jq .
```

Expected shape:

```json
{
  "id": 1,
  "name": "Alice Chen",
  "designation": "CEO",
  "salary": 200000.0,
  "reports": [
    {
      "id": 2,
      "name": "Bob Smith",
      "designation": "Engineering Manager",
      "salary": 150000.0,
      "reports": [
        {
          "id": 3,
          "name": "Carol Jones",
          "designation": "Software Engineer",
          "salary": 120000.0,
          "reports": []
        }
      ]
    }
  ]
}
```

### 5. Reporting chain (bottom-up)

```bash
curl -s http://localhost:8080/employees/3/chain | jq .
```

Returns `[Carol, Bob, Alice]`.

## Setup

### Prerequisites

- Java 24
- A Neon (or any PostgreSQL) database

### Configure credentials

```bash
cp .env.example .env
# edit .env and fill in your DB_URL, DB_USERNAME, DB_PASSWORD
```

`.env` format (plain Java properties, loaded via `spring.config.import`):

```properties
DB_URL=jdbc:postgresql://<host>/<dbname>?sslmode=require
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

### Run

```bash
./mvnw spring-boot:run
```

### Build (skip tests)

```bash
./mvnw clean package -DskipTests
```

## Swagger UI

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Project Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── controller/
│   └── EmployeeController.java      # HTTP layer, @Operation docs
├── service/
│   └── EmployeeService.java         # Business logic, cycle detection
├── repository/
│   └── EmployeeRepository.java      # JpaRepository<Employee, Long>
├── model/
│   └── Employee.java                # JPA entity with self-reference
├── dto/
│   ├── EmployeeRequest.java         # POST/PUT body (record)
│   ├── EmployeeResponse.java        # Response shape (record)
│   └── EmployeeTreeResponse.java    # Recursive tree node (record)
└── exception/
    ├── EmployeeNotFoundException.java
    ├── InvalidHierarchyException.java
    └── GlobalExceptionHandler.java  # @RestControllerAdvice, consistent error body
```

## Error Response Shape

```json
{
  "timestamp": "2026-11-25T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found: 99",
  "path": "/employees/99",
  "fieldErrors": null
}
```

Validation errors include a `fieldErrors` array with `field` and `message` per violation.
