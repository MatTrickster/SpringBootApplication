# Walmart / Salesforce Mid-Level SWE: Spring Boot Assessment Prep

## What the assessment looks like (from public candidate reports)

- Platform: HackerRank. Usually 2 parts: **1 DSA question** (LeetCode medium) + **1 Spring Boot "project" question**.
- Project question = a pre-built Maven Spring Boot repo in a browser IDE. You implement or fix
  controller/service/repository code until the **read-only tests in `src/test`** pass. You are scored on how many tests pass.
- Walmart (reported recently): a backend repo "debugging" task built on **Spring Profiles (dev, qa, staging)**: set the
  default profile, add per-profile properties, and finish service methods that return user details and default config.
  Some Walmart OAs now allow AI assistance in the project part.
- Salesforce: HackerRank OA covering CS fundamentals, DSA, and databases; mid-level loops add LLD/system design later.
- Classic HackerRank Spring problems: **Trades API**, **Weather API**, **Stocks / Ride-sharing / Employee CRUD**.

## HackerRank's official Spring Boot competencies

Basic: Spring MVC config, DI/IoC/ApplicationContext, REST CRUD, Initializr/Maven, **profiles for DEV/QA/PROD**,
auto-configuration report (`--debug`), unit and integration tests.
Intermediate: **H2 + Spring Data JPA**, robust REST APIs, embedded Tomcat packaging, **exception handling and custom
error responses**, **logging levels**.

## How this project maps to them

| Topic | Where |
|---|---|
| REST CRUD, status codes (201/200/400/404/405), query params | `trade/TradeController` |
| Layering + constructor injection | `trade/TradeService` |
| JPA, H2, derived query, JPQL, Query-By-Example | `trade/TradeRepository`, `TradeService.findAll` |
| Validation (`@Valid`, Bean Validation) | `trade/TradeRequest` |
| Global exception handling, ProblemDetail | `common/GlobalExceptionHandler` |
| Profiles, `spring.profiles.default`, `@ConfigurationProperties`, `@Profile` beans | `config/*`, `application-*.properties` |
| Logging levels per profile | `application-dev/staging.properties`, `@Slf4j` in service |
| Tests: `@SpringBootTest`, `@WebMvcTest`+`@MockitoBean`, `@DataJpaTest`, Mockito unit | `src/test/...` |

## Commands

```bash
./mvnw test                                    # what HackerRank effectively runs
./mvnw test -Dtest=TradeControllerIntegrationTest
./mvnw spring-boot:run                         # default profile = dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=qa
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug   # auto-configuration report
```

## Version gotchas (HackerRank repos are often older than this project)

This project uses Spring Boot 4.1 / Java 17. In the exam you may see Boot 2.x (Java 8/11) or 3.x.

| Thing | Boot 2.x | Boot 3.x | Boot 4.x |
|---|---|---|---|
| Persistence/validation imports | `javax.*` | `jakarta.*` | `jakarta.*` |
| Mock a bean in a slice test | `@MockBean` | `@MockBean` (3.4+: `@MockitoBean`) | `@MockitoBean` |
| `@WebMvcTest` package | `o.s.boot.test.autoconfigure.web.servlet` | same | `o.s.boot.webmvc.test.autoconfigure` |
| JUnit | 4 or 5 (check `@RunWith`) | 5 | 5/6 |

## Exam-day checklist

1. Read the README and **every test file first**. Tests are the spec (exact paths, status codes, JSON field names, ordering).
2. Run `mvn test` immediately to see the baseline failures.
3. Usual traps: missing `@ResponseStatus(CREATED)`; ids must start at 1; unordered lists (sort by id); 404 vs 500 for
   missing items; unmapped PUT/DELETE must be 405; `@RequestParam(required = false)`; wrong property key or profile filename
   (`application-qa.properties`, not `application_qa.properties`); `getActiveProfiles()` is empty when only the default is used.
4. Pass tests incrementally and leave nothing uncompiled. Partial credit is per test.

## Practice drills (do them without looking at the solution code)

1. Add `PUT /trades/{id}` with a 404 for unknown ids, then add tests.
2. Add `GET /trades/stats?userId=` that returns total bought/sold shares (reuse the JPQL query).
3. Add pagination: `GET /trades?page=&size=`, capped by `app.max-page-size`.
4. Build the **Weather API** from scratch in a new package (POST/GET/filter by date and city, sort, 404).
5. Walmart-style: add a `User` entity seeded via `data.sql` in `dev` only, plus `GET /users/{id}` and `GET /users/default`
   reading defaults from profile properties.
6. Time yourself: aim for about 45 minutes per drill.
