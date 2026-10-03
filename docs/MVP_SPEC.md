# Franquix — Franchiser as a Service (MVP specification)

Franquix is a logistics backend that lets any brand run a franchise network:
**franchise → branches → products (with stock)**. This document is the MVP scope.

## Domain

- A **franchise** has a name and a list of branches.
- A **branch** has a name and a list of offered products.
- A **product** has a name and a stock quantity.

## Acceptance criteria

1. Built with Spring Boot.
2. Endpoint to add a new franchise.
3. Endpoint to add a new branch to a franchise.
4. Endpoint to add a new product to a branch.
5. Endpoint to delete a product from a branch.
6. Endpoint to update a product's stock.
7. Endpoint that returns, for a given franchise, the product with the most stock in each branch,
   including which branch each product belongs to.
8. Persistence in a cloud-hosted data store (MongoDB Atlas).

## Extras (all in scope)

- Docker packaging (multi-stage `Dockerfile` + `docker-compose.yml`).
- Functional and reactive programming (WebFlux Router Functions + Handlers, `Mono`/`Flux` end to end).
- Rename a franchise, a branch and a product.
- Persistence provisioned with Terraform (Atlas M0 cluster, DB user, IP access list).
- Whole solution deployed to the cloud (the Docker image on Render, free plan).

## Stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5, Spring WebFlux (functional endpoints) |
| Architecture | Hexagonal (ports & adapters) |
| Persistence | MongoDB Atlas via Spring Data MongoDB Reactive |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, Reactor `StepVerifier`, `WebTestClient`, Testcontainers |
| Coverage | JaCoCo, build fails below 80 % |
| Packaging | Docker multi-stage, docker-compose (app + MongoDB) |
| IaC | Terraform (`mongodb/mongodbatlas` provider) |
| Cloud runtime | Render web service from the Dockerfile (`render.yaml`) |
| Security | One static bearer token (`API_TOKEN`) on every `/api` request |
| CI/CD | GitHub Actions (build, tests, Terraform checks); Render deploys `main` once CI passes |

## Endpoints (prefix `/api/v1`)

| Method | Path | Purpose | OK |
|---|---|---|---|
| POST | `/franchises` | Create franchise `{name}` | 201 |
| GET | `/franchises/{franchiseId}` | Franchise with branches and products | 200 |
| PATCH | `/franchises/{franchiseId}/name` | Rename franchise `{name}` | 200 |
| POST | `/franchises/{franchiseId}/branches` | Add branch `{name}` | 201 |
| PATCH | `/franchises/{franchiseId}/branches/{branchId}/name` | Rename branch `{name}` | 200 |
| POST | `/franchises/{franchiseId}/branches/{branchId}/products` | Add product `{name, stock}` | 201 |
| DELETE | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Delete product | 204 |
| PATCH | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Update stock `{stock}` | 200 |
| PATCH | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}/name` | Rename product `{name}` | 200 |
| GET | `/franchises/{franchiseId}/branches/top-stock-products` | Top-stock product per branch | 200 |

## Business rules

- Names are required, trimmed, 1–100 characters, not blank.
- Stock is an integer ≥ 0.
- Franchise names are globally unique; branch names are unique within a franchise; product names
  are unique within a branch → **409 Conflict**.
- The hierarchy in the path is enforced (branch belongs to franchise, product belongs to branch) → **404**.
- Malformed ids or bodies → **400**.
- Top-stock: 404 if the franchise does not exist; branches without products are omitted;
  ties are broken by product name ascending; results are ordered by branch name.
- Every error is an RFC 7807 `ProblemDetail` (`application/problem+json`).

## Delivery rules

- Git: one branch per phase (`chore/*`, `feat/*`, `test/*`, `build/*`, `infra/*`, `ci/*`, `docs/*`),
  Conventional Commits, merged into `main` with `--no-ff`, final tag `v1.0.0`.
- `./gradlew build` must pass after every phase.
- No secrets in the repository; `MONGODB_URI` is always provided through the environment.
