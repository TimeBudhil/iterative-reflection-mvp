# Iterative Reflection MVP

## Profiles

There is no default Spring profile; starting without one fails fast (no datasource).

- Local development: `./mvnw spring-boot:run` (the Maven plugin activates `dev`). Runs on a file-based H2 database in
  `./data/` (git-ignored; delete it to reset), with demo accounts, OpenAPI and Swagger UI. Sessions survive restarts.
  Running the jar by hand needs `--spring.profiles.active=dev`.
- Production: `SPRING_PROFILES_ACTIVE=prod` plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (and `ADMIN_EMAIL` /
  `ADMIN_PASSWORD` for the bootstrap admin). The `prod` profile trusts `X-Forwarded-For` / `X-Forwarded-Proto`
  (`server.forward-headers-strategy=native`), so the reverse proxy must set them; otherwise the per-IP login limit sees
  only the proxy's address. Set `FORWARD_HEADERS_STRATEGY=none` if the app is exposed directly.
