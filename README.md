# Invoice Service

A multi-tenant invoice management microservice built with Spring Boot and gRPC.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Security & Multi-Tenancy](#security--multi-tenancy)
   - [How Authentication Works](#how-authentication-works)
   - [Role-Based Access Control](#role-based-access-control)
   - [Multi-Tenancy](#multi-tenancy)
   - [Switching to Keycloak](#switching-to-keycloak)
5. [API Reference](#api-reference)
6. [How to Run](#how-to-run)
   - [Prerequisites](#prerequisites)
   - [Running with Security (default)](#running-with-security-default)
   - [Running without Security](#running-without-security)
   - [Testing with grpcurl](#testing-with-grpcurl)

---

## Overview

Invoice Service exposes a gRPC API for creating, retrieving, updating, listing, and deleting invoices. It is designed as a multi-tenant service — each request is authenticated and all data is strictly isolated per tenant.

**Tech stack:**

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Framework | Spring Boot 3.2.4 |
| API | gRPC + Protocol Buffers |
| Security | Spring Security + JWT (Keycloak-ready) |
| Build | Maven |
| Testing | JUnit 5, Mockito, AssertJ |

---

## Architecture

The service follows Clean Architecture with dependencies pointing inward only:

```
┌─────────────────────────────────────┐
│  api/          (gRPC adapters)      │  ← outermost: depends on service
├─────────────────────────────────────┤
│  service/      (business logic)     │  ← depends on domain + repository
├─────────────────────────────────────┤
│  repository/   (data access)        │  ← implements domain contracts
├─────────────────────────────────────┤
│  domain/       (entities, rules)    │  ← innermost: no outward dependencies
└─────────────────────────────────────┘
```

Security infrastructure lives in `security/` and integrates at the `api/` boundary via a gRPC interceptor, keeping the domain and service layers clean.

---

## Project Structure

```
src/main/java/se/kleer/invoice/
├── Application.java
├── api/
│   └── InvoiceGrpcService.java          # gRPC endpoint implementation
├── domain/
│   ├── Invoice.java                     # Invoice entity
│   ├── InvoiceItem.java                 # Line item value object
│   ├── InvoiceStatus.java               # Status enum
│   └── security/
│       ├── Permission.java              # Fine-grained permission enum
│       ├── Role.java                    # Role enum with permission sets
│       └── TokenClaims.java             # Validated token claims record
├── repository/
│   ├── InvoiceRepository.java           # Repository interface (tenant-scoped)
│   └── InMemoryInvoiceRepository.java   # In-memory implementation
├── security/
│   ├── AuthTenantInterceptor.java       # gRPC interceptor: auth + tenant setup
│   ├── MockTokenValidator.java          # Dev/test token validator
│   ├── PermissionResolver.java          # Expands roles → Spring authorities
│   ├── SecurityConfig.java              # Enables method-level security
│   ├── TenantContext.java               # Thread-local tenant ID holder
│   └── TokenValidator.java             # Interface (swap point for Keycloak)
└── service/
    └── InvoiceService.java              # Business logic + @PreAuthorize guards
```

---

## Security & Multi-Tenancy

### How Authentication Works

Every gRPC request must carry a bearer token in the `authorization` metadata header:

```
authorization: Bearer <token>
```

The `AuthTenantInterceptor` runs before every service method and performs these steps:

```
Request arrives
      │
      ▼
AuthTenantInterceptor
      │
      ├── 1. Extract "Authorization: Bearer <token>" from gRPC metadata
      │
      ├── 2. Call TokenValidator.validate(token)
      │         │
      │         ├── mock-auth profile → MockTokenValidator
      │         └── prod profile      → KeycloakTokenValidator (future)
      │
      ├── 3. On success: extract TokenClaims { subject, tenantId, roles }
      │
      ├── 4. Expand roles → permissions via PermissionResolver
      │         e.g. ADMIN → [INVOICE_READ, INVOICE_CREATE, INVOICE_UPDATE, INVOICE_DELETE]
      │
      ├── 5. Set Spring SecurityContext (subject + permissions)
      │
      ├── 6. Set TenantContext (tenantId) on current thread
      │
      └── 7. Proceed to service method
                │
                ▼
         @PreAuthorize("hasAuthority('INVOICE_CREATE')")
         InvoiceService.createInvoice(...)
                │
                ▼
         InvoiceRepository (all queries filtered by tenantId)
```

If the token is missing, malformed, or invalid, the interceptor closes the call immediately with gRPC status `UNAUTHENTICATED`. The service layer is never reached.

### Role-Based Access Control

Authorization checks are **permission-based**, not role-based. Service methods are guarded by `@PreAuthorize` annotations that check for specific permissions:

```java
@PreAuthorize("hasAuthority('INVOICE_READ')")
@PreAuthorize("hasAuthority('INVOICE_CREATE')")
@PreAuthorize("hasAuthority('INVOICE_UPDATE')")
@PreAuthorize("hasAuthority('INVOICE_DELETE')")
```

Roles are named bundles of permissions defined in `Role.java`:

| Role | INVOICE_READ | INVOICE_CREATE | INVOICE_UPDATE | INVOICE_DELETE |
|---|:---:|:---:|:---:|:---:|
| `ADMIN` | yes | yes | yes | yes |
| `MANAGER` | yes | yes | yes | no |
| `USER` | no | no | no | no |

**Adding a new role** requires one change only — a new enum constant in `Role.java` with its permission set. No service method guards ever need updating.

**Extending a role's access** requires updating its `EnumSet` in `Role.java`. Again, no other changes needed.

### Multi-Tenancy

Tenant isolation is enforced at two levels:

1. **Token level** — the `tenantId` is extracted from the validated token. A user cannot claim a tenant they do not belong to.
2. **Repository level** — every read operation is filtered by `tenantId`. Looking up an invoice that belongs to a different tenant returns empty, exactly as if the invoice did not exist.

The tenant ID flows through the request like this:

```
Token claims.tenantId()
      │
      ▼
TenantContext.setTenantId(...)    ← set by AuthTenantInterceptor
      │
      ▼
TenantContext.getTenantId()       ← read by InvoiceGrpcService
      │
      ▼
InvoiceService.createInvoice(tenantId, ...)
      │
      ▼
invoice.setTenantId(tenantId)     ← stamped on every new invoice
      │
      ▼
repository.findById(id, tenantId) ← all reads are tenant-scoped
```

`TenantContext` is a thread-local that is always cleared after each request, preventing leaks between calls in the gRPC thread pool.

### Switching to Keycloak

The `TokenValidator` interface is the single swap point. To add Keycloak support:

1. Add the Keycloak/Nimbus JWT dependency to `pom.xml`.
2. Create `KeycloakTokenValidator implements TokenValidator` annotated with `@Profile("prod")`.
3. Map the JWT claims (`sub`, custom tenant claim, `realm_access.roles`) to `TokenClaims`.
4. Set `spring.profiles.active=prod` in production configuration.

Nothing else in the codebase changes.

---

## API Reference

The service is defined in `src/main/resources/proto/invoice/v1/invoice.proto`.

| RPC | Request | Response | Required permission |
|---|---|---|---|
| `CreateInvoice` | `CreateInvoiceRequest` | `CreateInvoiceResponse` | `INVOICE_CREATE` |
| `GetInvoice` | `GetInvoiceRequest` | `GetInvoiceResponse` | `INVOICE_READ` |
| `ListInvoices` | `ListInvoicesRequest` | `ListInvoicesResponse` | `INVOICE_READ` |
| `UpdateInvoice` | `UpdateInvoiceRequest` | `UpdateInvoiceResponse` | `INVOICE_UPDATE` |
| `DeleteInvoice` | `DeleteInvoiceRequest` | `DeleteInvoiceResponse` | `INVOICE_DELETE` |

---

## How to Run

### Prerequisites

- Java 23
- Maven 3.9+

### Running with Security (default)

The application starts with the `mock-auth` profile active by default, which uses `MockTokenValidator`. No external identity provider is needed.

```bash
mvn spring-boot:run
```

The gRPC server starts on port `9090`.

The mock token format is:

```
tenantId|userId|ROLE1,ROLE2
```

Examples:

| Token | Tenant | User | Access |
|---|---|---|---|
| `tenant-abc\|user-1\|ADMIN` | tenant-abc | user-1 | Full access |
| `tenant-abc\|user-2\|MANAGER` | tenant-abc | user-2 | Read, create, update |
| `tenant-abc\|user-3\|USER` | tenant-abc | user-3 | No invoice access |
| `tenant-xyz\|user-4\|ADMIN` | tenant-xyz | user-4 | Full access, isolated from tenant-abc |

### Running without Security

> **Note:** Disabling security is intended for local debugging only. Never run without security in production.

To bypass authentication, exclude the `AuthTenantInterceptor` bean by running with a profile that does not provide a `TokenValidator` implementation. The simplest approach is to create a no-op validator for a dedicated profile.

Alternatively, run with a system property to override the active profile and provide a bypass implementation:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=no-auth"
```

Then add a `NoOpTokenValidator` in your local dev sources:

```java
@Component
@Profile("no-auth")
public class NoOpTokenValidator implements TokenValidator {
    @Override
    public TokenClaims validate(String token) {
        // Accept any token, assign a fixed dev tenant and full access
        return new TokenClaims("dev-user", "dev-tenant", Set.of(Role.ADMIN));
    }
}
```

### Testing with grpcurl

Install [grpcurl](https://github.com/fullstorydev/grpcurl), then call `CreateInvoice` with an admin token:

```bash
grpcurl \
  -plaintext \
  -H 'authorization: Bearer tenant-abc|user-1|ADMIN' \
  -d '{
    "customer_id": "cust-001",
    "customer_name": "Acme Corp",
    "issue_date": {"seconds": 1711411200},
    "due_date":   {"seconds": 1714003200},
    "currency": "SEK",
    "items": [
      {
        "description": "Consulting services",
        "quantity": 5,
        "unit_price_cents": 150000
      }
    ]
  }' \
  localhost:9090 se.ts.invoice.v1.InvoiceService/CreateInvoice
```

To verify tenant isolation, repeat the same request with a different tenant in the token (`tenant-xyz|user-1|ADMIN`) — invoices created under `tenant-abc` will not be visible.
