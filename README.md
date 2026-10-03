# Seamline

A sewing-line digital twin for RMG production. One Spring Boot application: it serves the
REST API **and** the stakeholder front end, so there is a single thing to run and no CORS
setup before a demo.

Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · H2 (Postgres or MySQL optional)

---

## Run it

```bash
cd seamline
mvn spring-boot:run
```

Then open **http://localhost:8080** for the landing page, and **http://localhost:8080/app.html**
(or the landing page's "Sign in" / "Enter Seamline" buttons) to sign in.

| Email                          | Password      | Role                | Lands on            |
|--------------------------------|---------------|---------------------|---------------------|
| `farhana.akter@seamline.com`   | `seamline123` | Industrial Engineer | Dashboard           |
| `kamal.hossain@seamline.com`   | `seamline123` | Line Supervisor     | Shift overview      |
| `rezaul.karim@seamline.com`    | `seamline123` | Production Director | Executive summary   |
| `rahima.begum@seamline.com`    | `seamline123` | Sewing Operator     | My station           |

Each account's role decides its workspace, so sign-in goes straight to it — there's no
picker step. `Esc` signs out.

The database is in-memory H2, seeded on first start with two styles and their precedence
graphs, four production lines, a 14-operator roster with a skill matrix, an order book, and
one simulated shift per line — so every screen has real numbers immediately. H2 console:
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:seamline`, user `sa`, no password).

To use Postgres instead: `docker compose up -d` then
`mvn spring-boot:run -Dspring-boot.run.profiles=postgres`.

To use MySQL instead (e.g. XAMPP, root user, no password, `localhost:3306`): create the
`seamline` database — `seamline_mysql.sql` at the repo root has the full schema plus the same
demo data `DataSeeder` creates, or just let Hibernate create it (`ddl-auto: update`) against an
empty `seamline` schema — then `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`.

---

## The Industrial Engineer feature

All five IE screens are backed by the API. Nothing on them is computed in the browser. "Export
report" (on Dashboard and Executive summary) downloads a CSV built client-side from whatever
the screen is currently showing — no extra endpoint needed, since the API already returned
everything in it.

| Screen | Endpoint | What the backend does |
|--------|----------|------------------------|
| Dashboard | `GET /api/dashboard`, `POST /api/dashboard/rerun` | Reads the latest stored shift for the line, or balances and simulates a new one and saves it |
| Line simulation | `POST /api/simulation/run` | Balances with the chosen algorithm and settings, simulates one 480-minute shift, returns the plan and the result. The playback in the browser replays that result — it does not re-simulate |
| Style breakdown | `GET /api/styles/{code}`, `POST /api/styles/{code}/operations`, `POST /api/styles/{code}/operations/import` | Operation bulletin: SMV, machine, minimum grade, precedence links (the graph is laid out client-side from those links). "Add operation" and "Import from CSV" write new operations and re-check the whole precedence graph is still acyclic before saving |
| Operators | `GET /api/operators` | Roster and the per-machine skill matrix |
| Scenarios | `GET /api/simulation/scenarios?seed=42` | Runs five plans against one seed, so any difference comes from the variable named in the scenario and not from luck |

The supervisor's shift overview and roster, the operator's own station, and the
management line/order screens are wired to the same API. Approvals and the operator's weekly
chart are still local demo data — there is no entity behind them yet.

### Full endpoint list

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/login` | none |
| GET | `/api/auth/me` | bearer |
| GET | `/api/dashboard` | bearer |
| POST | `/api/dashboard/rerun` | bearer |
| POST | `/api/simulation/run` | bearer |
| GET | `/api/simulation/scenarios` | bearer |
| GET | `/api/styles`, `/api/styles/{code}` | bearer |
| POST | `/api/styles/{code}/operations` | bearer, ADMIN or INDUSTRIAL_ENGINEER |
| POST | `/api/styles/{code}/operations/import` | bearer, ADMIN or INDUSTRIAL_ENGINEER |
| GET | `/api/operators` | bearer |
| GET | `/api/lines`, `/api/orders` | bearer |

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"farhana.akter@seamline.com","password":"seamline123"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/dashboard -H "Authorization: Bearer $TOKEN" | jq
curl -s "http://localhost:8080/api/simulation/scenarios?seed=42" -H "Authorization: Bearer $TOKEN" | jq
```

Errors always come back as `{ timestamp, status, error, message, path, fieldErrors }`.

---

## How it fits together

```
src/main/java/com/seamline/
  controller/   Auth, Dashboard, Simulation, Style, Operator, Factory, GlobalExceptionHandler
  service/      AuthService, DashboardService, LineSimulationService, FactoryService
    balancing/  BalancingStrategy -> RankedPositionalWeight | GreedyTopological
                LineBalancer, LinePlan, PlannedStation, WorkerProfile
    simulation/ ShiftSimulator (discrete-event), SimulationConfig/Outcome, DeterministicRandom
  domain/       BaseEntity -> Employee, ProductionLine, Style, Operation, Operator,
                OperatorSkill, ShiftRun, StationResult, Order (+ enums)
  repository/   Spring Data JPA interfaces
  security/     JwtService, JwtAuthenticationFilter, EmployeeDetailsService, SeamlineUserDetails
  config/       SecurityConfig, SeamlineProperties, DataSeeder
  dto/          request/response records
src/main/resources/static/index.html    landing page, served at "/"
src/main/resources/static/app.html      the sign-in / stakeholder workspace app, served at "/app.html"
```

**Dashboard request flow:** controller → `DashboardService` → `LineBalancer` (which asks a
`BalancingStrategy` for the operation order) → `ShiftSimulator` → `ShiftRun` saved through
JPA → mapped into `DashboardResponse`.

The simulation is a real discrete-event model, not a formula. Bundles of 20 pieces move from
station to station, a station can only pass work on when the next buffer has room, and time
jumps from event to event. Blocking and starving are *results* of the model, which is why the
bottleneck alert means something — and why dropping the buffer to 1 on the Line simulation
screen visibly costs output.

### OOP points worth naming in the report

| Concept | Where |
|---------|-------|
| Inheritance | `BaseEntity` → every entity; `AbstractBalancingStrategy` → the two algorithms |
| Polymorphism | `LineBalancer` depends only on the `BalancingStrategy` interface |
| Strategy pattern | Switching `rpw` to `greedy` changes the whole layout, with no other edits |
| Encapsulation | `Operator.efficiencyFor()`, `Style.totalSmv()`, `StationResult.classify()`, `Order.progressPercent()` — behaviour sits with the data |
| Adapter | `SeamlineUserDetails` keeps Spring Security out of the `Employee` entity |
| Open/closed | A new algorithm is one new `@Component`; `BalancingStrategyRegistry` finds it automatically |
| Immutability | DTOs and simulation values are records; `WorkerProfile` is an immutable snapshot |
| Single source of truth | The balancing and simulation code that used to live in the HTML was deleted — the browser only draws |

---

## Tests

```bash
mvn test
```

* `LineBalancerTest` — every operation placed once, precedence never violated, no station
  mixes machine types.
* `ShiftSimulatorTest` — a shift produces pieces, every station-minute is accounted for, and
  the same seed always gives the same shift.
* `AuthAndDashboardIT` — sign in, reject a wrong password, refuse the API without a token,
  and check the dashboard, style, operator, simulation, scenario, line and order payloads
  end to end, plus that the front end is served.

---

## Notes

* Set `SEAMLINE_JWT_SECRET` in the environment before running this anywhere but a laptop.
* `seamline.cors.allowed-origins` is `"*"` so the HTML also works if opened as a file during
  development. Narrow it for anything shared.
* Approvals, the operator schedule and the weekly performance chart are the remaining
  browser-only pieces; each needs an entity and a repository to become real.
