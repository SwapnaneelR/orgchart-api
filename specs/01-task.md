I'm extending this Spring Boot project (Spring Boot 4.0.8, Java 24, Maven, PostgreSQL on Neon) into an org-hierarchy REST API. Read the existing code first. Right now it has one Employee entity (id, name, salary, designation), a JpaRepository, and two endpoints: GET /employees/all and POST /employees/create.

Implement the following and keep everything on PostgreSQL. Do NOT add H2 or any in-memory DB, and do NOT write new tests.

1. Security fix (do this first)
   - src/main/resources/application.properties has the Neon URL, username and password hardcoded. Replace them with ${DB_URL}, ${DB_USERNAME}, ${DB_PASSWORD}.
   - Load a local .env file (e.g. spring.config.import=optional:file:.env[.properties], or the dotenv-java dependency already in the pom; pick one and remove whichever isn't used).
   - Add .env to .gitignore and create a .env.example with placeholder values.
   - Remove the unused spring-boot-h2console dependency from pom.xml.

2. Org hierarchy
   - Add a self-referencing relationship to Employee: @ManyToOne(fetch = LAZY) manager (nullable, for the CEO) and @OneToMany(mappedBy = "manager") reports.
   - Prevent invalid hierarchies: an employee can't be their own manager, and setting a manager must not create a cycle (walk up the chain to check).

3. Layered design + DTOs
   - Restructure into controller / service / repository / model / dto / exception packages. Business logic goes in an EmployeeService, not the controller.
   - Use request/response DTOs (Java records are fine), never return entities directly, to avoid lazy-loading and infinite JSON recursion. The response includes managerId and managerName.

4. Endpoints (replace the old /all and /create)
   - POST   /employees              → 201 Created + Location header. Body: name, designation, salary, managerId (optional)
   - GET    /employees              → paginated + sortable via Spring Data Pageable (?page=0&size=10&sort=name,asc)
   - GET    /employees/{id}
   - PUT    /employees/{id}
   - DELETE /employees/{id}         → 204. Reassign the deleted employee's direct reports to the deleted employee's manager.
   - GET    /employees/{id}/reports → direct reports
   - GET    /employees/{id}/chain   → reporting chain from the employee up to the top
   - GET    /employees/{id}/tree    → recursive nested tree (employee + reports, recursively)

5. Validation + errors
   - Add spring-boot-starter-validation. Use @NotBlank name/designation, @Positive salary, and @Valid on request bodies.
   - Add a @RestControllerAdvice global exception handler with a consistent JSON error body (timestamp, status, error, message, path, and field errors for validation): 404 for an unknown employee/manager, 400 for validation errors and invalid hierarchy (self-manager or cycle).

6. API docs
   - Add springdoc-openapi with a version that is compatible with Spring Boot 4 (check this; older 2.x versions target Boot 3).
   - Swagger UI at /swagger-ui.html. Add brief @Operation summaries on the endpoints.

7. README.md
   Write a clear README covering:
   - Overview
   - Tech stack
   - Data model (a short description of the self-referencing Employee)
   - Endpoints table (method, path, description)
   - Example curl requests for creating a small hierarchy (CEO → manager → engineer) and fetching the tree
   - Setup: Java 24, Neon/Postgres, copy .env.example to .env, run with ./mvnw spring-boot:run
   - Swagger UI link
   - Project structure

Verification
   - Make sure ./mvnw clean package -DskipTests compiles.
   - If a .env with DB credentials exists, start the app and hit each endpoint with curl to confirm it works. Otherwise, tell me what you couldn't verify.

Git
   - Make small, logical commits (security fix, hierarchy, DTOs/service, endpoints, validation/errors, Swagger, README).
   - Don't push. I'll review first.

At the end, summarise what changed and anything I still need to do manually.