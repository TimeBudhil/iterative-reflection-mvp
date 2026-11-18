# Performance check (N1, N6.1)

A run-by-hand latency check. It is **not** part of `./mvnw verify` or CI: it lives outside `src/` and is only
started by `perf/run-perf.sh`.

## Run it

```
cd mvp
perf/run-perf.sh            # 300 students, 4 client threads
perf/run-perf.sh 300 8      # other student count / client concurrency
```

Needs JDK 25+ (`java` on the PATH), `curl`, `unzip`. Environment: `PORT` (default 18080), `HEAP` (default
`-Xmx512m`). Takes about 2 minutes. Exit code 0 only if every p95 target is met and no request returned an
unexpected status; the table is also saved to `target/perf/results.txt` (app log: `target/perf/app.log`).

What it does:

1. `./mvnw package -DskipTests`, then starts the jar with `-Xmx512m` (dev profile: seed careers, demo accounts) on a
   fresh file-based H2 database (`AUTO_SERVER`, so the script can reach the same database as the app).
2. `PerfCheck.java` seeds, with plain SQL, on top of the 27 seed careers: 300 students (password `demo`), each with
   20 reflections (1 version and 5 answers each), page progress, 2 abandoned careers, affinity contributions, a link
   to the demo counselor, and an intake for every second student. That is 6,000 reflections, 30,000 answers.
3. Signs everyone in over HTTP (real sessions, CSRF cookie and header) and drives the API: feed (first call that
   picks a card, then stored card), experience, page viewed, reflect, edit, abandon, intake, reflection list and
   history, counselor dashboard (list at three page positions, detail for all 300 students), admin metrics, audit and
   counselor list, plus sign-in.
4. Prints p50 / p95 / max per endpoint. Targets: p95 < 100 ms for `GET /api/feed/next`, p95 < 300 ms for every
   other endpoint.

## Results

Run on a developer laptop (macOS, Java 26), client and server on the same machine, H2 file database, app on
`-Xmx512m`, 4 concurrent client threads. The app started and served all of this within the 512 MB heap.

| Endpoint | n | p50 ms | p95 ms | max ms | target | result |
|---|---|---|---|---|---|---|
| GET /api/admin/audit | 20 | 5.3 | 16.3 | 30.4 | < 300 | PASS |
| GET /api/admin/counselors | 20 | 7.2 | 25.5 | 63.7 | < 300 | PASS |
| GET /api/admin/pilot-metrics | 20 | 25.5 | 62.5 | 82.9 | < 300 | PASS |
| GET /api/careers/{id}/experience | 600 | 2.2 | 7.7 | 76.9 | < 300 | PASS |
| GET /api/careers/{id}/reflection/versions | 300 | 3.2 | 10.0 | 75.1 | < 300 | PASS |
| GET /api/counselor/me | 20 | 3.2 | 5.7 | 5.8 | < 300 | PASS |
| GET /api/counselor/students (last page) | 20 | 82.6 | 113.7 | 118.4 | < 300 | PASS |
| GET /api/counselor/students (page of 20) | 20 | 83.0 | 104.2 | 106.1 | < 300 | PASS |
| GET /api/counselor/students (size 100) | 20 | 85.1 | 102.8 | 120.8 | < 300 | PASS |
| GET /api/counselor/students/{id} | 300 | 5.7 | 23.6 | 79.2 | < 300 | PASS |
| GET /api/feed/next (after abandoning) | 100 | 2.8 | 8.0 | 21.4 | < 100 | PASS |
| GET /api/feed/next (after reflecting) | 100 | 3.5 | 9.2 | 61.3 | < 100 | PASS |
| GET /api/feed/next (card stored) | 900 | 2.9 | 8.9 | 24.1 | < 100 | PASS |
| GET /api/feed/next (first call, picks card) | 300 | 5.5 | 19.8 | 68.5 | < 100 | PASS |
| GET /api/intake | 600 | 2.1 | 7.8 | 23.4 | < 300 | PASS |
| GET /api/intake/questions | 600 | 1.9 | 5.8 | 16.4 | < 300 | PASS |
| GET /api/me | 600 | 2.0 | 6.3 | 15.9 | < 300 | PASS |
| GET /api/reflection-questions | 600 | 1.8 | 6.0 | 76.1 | < 300 | PASS |
| GET /api/students/me/counselors | 600 | 2.5 | 8.3 | 76.1 | < 300 | PASS |
| GET /api/students/me/reflections (page of 20) | 600 | 3.2 | 10.3 | 34.5 | < 300 | PASS |
| GET /api/students/me/reflections (size 100) | 600 | 3.1 | 9.2 | 18.9 | < 300 | PASS |
| POST /api/auth/signin (admin) | 1 | 105.6 | 105.6 | 105.6 | < 300 | PASS |
| POST /api/auth/signin (counselor) | 1 | 88.5 | 88.5 | 88.5 | < 300 | PASS |
| POST /api/auth/signin (student) | 300 | 91.6 | 113.5 | 178.7 | < 300 | PASS |
| POST /api/careers/{id}/abandon | 100 | 2.4 | 10.9 | 13.7 | < 300 | PASS |
| POST /api/careers/{id}/pages/{n}/viewed | 400 | 2.3 | 7.5 | 23.9 | < 300 | PASS |
| POST /api/careers/{id}/reflection | 100 | 4.9 | 61.1 | 208.6 | < 300 | PASS |
| POST /api/intake | 40 | 4.1 | 30.1 | 30.1 | < 300 | PASS |
| PUT /api/careers/{id}/reflection | 100 | 4.5 | 16.3 | 70.2 | < 300 | PASS |

All targets met with no production changes. The slowest read is the counselor student list (about 100 ms p95 for
300 linked students; it pages in memory over one batched query per source, so cost grows with linked-student
count, not page number). Sign-in is dominated by BCrypt (about 90 ms each, by design).

## Caveats

- H2 on the same host removes network latency to the database; re-run against Postgres (`DB_URL` style
  `spring.datasource.url` override in `run-perf.sh`) before trusting absolute numbers for production hardware.
- Everything runs the dev profile (demo seed on, springdoc on); the perf students reuse the demo student's BCrypt
  hash.
- The write phase mutates data (reflects, abandons, intakes); each run starts from a fresh database.
