# Project Development Guidelines

## Architecture Principles
- Follow Clean Architecture (entities, use cases, interfaces, frameworks as layers)
- Depend inward only — outer layers depend on inner layers, never the reverse

## Coding Standards
- Follow SOLID principles at all times
- Prefer composition over inheritance
- All public methods must have Javadoc comments
- No magic numbers — use named constants

## Naming Conventions
- Classes: PascalCase
- Methods/variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase, e.g. com.yourcompany.module

## Testing
- Write unit tests for all business logic (JUnit 5 + Mockito + AssertJ)
- Aim for 80%+ test coverage
- Follow the Given-When-Then pattern

## Error Handling
- Never swallow exceptions silently
- Use custom exception classes for domain errors
- Always log exceptions with meaningful context

## Patterns to Use
- Repository pattern for data access
- Factory pattern for object creation
- Use DTOs to transfer data across layers

## Tools & Libraries
- Build: Maven
- Testing: JUnit 5, Mockito
- Logging: SLF4J + Logback