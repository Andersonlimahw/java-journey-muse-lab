# feat: Java 26 + Spring Boot 4.1.1 (dep bump, vuln fixes)

Moves the build off the EOL Boot 3.3.1 / Java 21 baseline onto the current
stable line, aligned with the local `26.0.2-tem` toolchain.
Full rationale: `docs/adr/0003-java-26-spring-boot-4-upgrade.md`.

## Build baseline

| Concern | Before | After |
|---|---|---|
| Spring Boot | 3.3.1 (OSS EOL) | **4.1.1** |
| Java | 21 | **26** (`.java-version`, `java.version`) |
| Lombok | 1.18.38 (breaks on JDK 26) | **1.18.48** (official JDK 26 support) |
| Spring Framework / Tomcat / Hibernate | 6.1 / 10.1 / 6.5 | **7.0.9 / 11.0.24 / 7.4.5.Final** |
| H2 / Flyway | 2.2.x / 10.x | **2.4.240 / 12.4.0** |
| Tests | Jackson 2, old MockMvc slice | `spring-boot-webmvc-test`, Jackson 3 (`tools.jackson`), new `@AutoConfigureMockMvc` package |

Main sources compile **unchanged** (`jakarta.*` since Boot 3).
Picks up upstream fixes for CVE-2024-38807/38808/38809, CVE-2025-22235,
CVE-2025-41248/41249 (representative list, see ADR).

## Test-only adjustments (Boot 4)

- `spring-boot-webmvc-test` added (MockMvc slice split out of `starter-test`)
- 5 test files: `com.fasterxml` → `tools.jackson` `ObjectMapper`
  (Boot 4 auto-configures the Jackson 3 bean)
- 6 test files: `@AutoConfigureMockMvc` import moved to
  `org.springframework.boot.webmvc.test.autoconfigure`
- Fixed latent isolation bug: `tripRepository.deleteAll()` in trips tests
  assumed an empty shared DB and violated FKs under the new test execution
  order — dropped, assertions are per-trip scoped

## Quality gate

```text
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0  — BUILD SUCCESS
on 26.0.2-tem, default order AND -Dsurefire.runOrder=reversealphabetical
```

`./scripts/validate-api.sh` (29 requests) left for reviewer/CI — the build
sandbox blocks Java socket bind, so `spring-boot:run` can't start there.
REST contract untouched; MockMvc covers all five slices.

## Rollback

Two-file revert (`pom.xml`, `.java-version`) returns to Boot 3.3.1 / Java 21.
