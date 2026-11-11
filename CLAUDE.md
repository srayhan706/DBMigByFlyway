# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A small Spring Boot CRUD API (products and categories on PostgreSQL) built as a learning project for Flyway database migrations. The schema was grown on purpose in small numbered steps, so keep changes incremental: one migration per schema change, with an explanation of what it does and why.

Stack: Spring Boot 4.1.2, Java 21, Spring Data JPA, Flyway, PostgreSQL, Maven wrapper. No Lombok.

## Commands

Maven is not installed globally; always use the wrapper. Run everything from the project root, because the `.env` file is resolved relative to the working directory.

```bash
./mvnw spring-boot:run                              # run the app on port 8080 (applies pending migrations first)
./mvnw -DskipTests package                          # build the jar without touching the database
./mvnw test                                         # run all tests
./mvnw test -Dtest=DbMigCrudApplicationTests        # run one test class
./mvnw test -Dtest=DbMigCrudApplicationTests#contextLoads   # run one test method
```

- The only test is a `@SpringBootTest` context load. It connects to the real database from `.env` and runs Flyway against it, so `./mvnw test` and `./mvnw package` fail unless PostgreSQL is up and `.env` is filled in. Use `-DskipTests` for a compile-only check.
- There is no linter or formatter configured.

## Configuration

- Database credentials live in `.env` (gitignored; `.env.example` is the template): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- `application.properties` loads that file with `spring.config.import=optional:file:.env[.properties]` and builds the datasource from `${DB_...}` placeholders. Real environment variables with the same names override the file.
- The database named in `DB_NAME` must already exist. Flyway creates tables, not the database.

## Schema ownership: Flyway writes, Hibernate only validates

`spring.jpa.hibernate.ddl-auto=validate`. Hibernate never creates or alters tables. Startup order is: Flyway applies pending files from `src/main/resources/db/migration`, then Hibernate checks the entities against the tables and aborts startup on a mismatch.

Every schema change is therefore made by hand in two places that must agree:

1. A new `V<n>__<description>.sql` file (next free version; V1 to V5 exist).
2. The matching entity change, plus the service if the field should be writable through the API.

Rules that follow from this:

- Never edit, rename or delete a migration that has already been applied. V1 to V5 are applied to the development database, and Flyway refuses to start when a checksum changes. Fix mistakes with a new migration.
- `validate` checks tables, columns and types only. Unique constraints, indexes and defaults are not checked, so they exist only in the SQL. Annotations such as `unique = true` are documentation.
- Migrations are PostgreSQL-specific SQL. Constraints and indexes are named explicitly: `uk_<table>_<column>`, `fk_<table>_<target>`, `idx_<table>_<column>`.
- When adding `NOT NULL` columns or constraints, account for rows that already exist (see V3 for the default pattern, V5 for a nullable foreign key).

## Code architecture

Packages are by feature under `com.example.dbmigcrud`: `product` (full CRUD at `/api/products`) and `category` (read-only `GET /api/categories`; rows are seeded by migration V5).

Things that are not obvious from a single file:

- **Entities are the API contract.** There are no DTOs. `Product` is both the JPA entity and the JSON request/response body, and Bean Validation annotations sit on the entity fields. Adding a column changes the JSON shape.
- **PUT is a full replacement.** `ProductService.update` copies fields one by one from the request onto the loaded entity. A new field must be added there or PUT will silently ignore it; a field omitted from a PUT body is reset to its default or null.
- **Category is set by id.** Requests send `{"category": {"id": 3}}`. `ProductService.resolveCategory` loads the real row and returns 400 for a missing or unknown id, so the foreign key is never violated at the database level.
- **`ProductExceptionHandler` maps every `DataIntegrityViolationException` to 409** with a message about a duplicate product name. That is correct only while `uk_products_name` is the sole constraint reachable from the API. Revisit it when adding constraints.
- **`spring.jpa.open-in-view=false`** and entities are serialized directly, so associations must be loaded before the controller returns. `Product.category` relies on the default eager fetch of `@ManyToOne`; switching it to lazy would break JSON serialization.

## Conventions

- Tabs for indentation, constructor injection, explicit getters and setters.
- Spring Boot 4 starter names differ from Boot 3: `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, and per-starter `-test` artifacts. Flyway also needs `flyway-database-postgresql`.
- The parent version must be the plain `4.1.2`. Spring Initializr generated `4.1.2.RELEASE`, which does not exist on Maven Central.

## Docs

`docs/Flyway-vs-Hibernate-Migration-Guide.pdf` explains migrations V1 to V5 and compares Flyway with Hibernate schema generation. It is a generated artifact with no build script in the repository, so it does not update when migrations are added.
