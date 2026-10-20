# Incident Management Portal

React 19 + Spring Boot 3.5 (Java 21) + MySQL 8.4 · JWT auth · role-based access (USER / ADMIN)

## Run locally

### 1. Database
```sql
CREATE DATABASE incident_management;
```

### 2. Backend (http://localhost:8080)
```bash
cd backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# edit it: DB password, app.jwt.secret (32+ chars), seed passwords
mvn spring-boot:run
```
Set `app.email.allowed-domain=yourcompany.com` to accept only company email addresses when admins create users
(comma separate several domains; blank = any domain). Email is optional for the seeded accounts.
On first start the admin/user accounts from `app.seed.*` are created (a blank seed password = not created).
Secrets can also come from environment variables: `DB_PASSWORD`, `JWT_SECRET`, `SEED_ADMIN_PASSWORD`, `SEED_USER_PASSWORD`.

### 3. Frontend (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev        # Vite proxies /api -> http://localhost:8080
```

### Docker (optional)
```bash
cp .env.example .env     # edit values
docker compose up --build
# open http://localhost:3000
```

## Tests
```bash
cd backend
mvn test                 # JUnit 5 + Mockito service tests, JWT tests, H2 integration test (no MySQL needed)
mvn verify               # also enforces >= 80% line coverage on the service package (JaCoCo)
# report: backend/target/site/jacoco/index.html
cd ../frontend && npm test
```

## Roles & rules
| Action | USER | ADMIN |
|---|---|---|
| Login, create incident | ✔ | ✔ |
| View incidents | own (created by / assigned to) | all |
| Update incident fields | own (creator / assignee) | any |
| Change status | assignee; creator may close a RESOLVED one | any |
| Assign, delete (soft), manage users | ✘ | ✔ |

Lifecycle is strictly `OPEN → IN_PROGRESS → RESOLVED → CLOSED`; closed incidents are read-only.

## API
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/login` | `{username,password}` → `{token,userId,username,role}` |
| POST | `/api/incidents` | create (status OPEN) |
| GET | `/api/incidents?status=&priority=&application=` | filters combine with AND |
| GET | `/api/incidents/{id}` | |
| PUT | `/api/incidents/{id}` | title, description, priority, category |
| PUT | `/api/incidents/{id}/assign?userId=` | admin |
| PUT | `/api/incidents/{id}/status` | `{status}` |
| DELETE | `/api/incidents/{id}` | admin, soft delete |
| GET/POST | `/api/users` | admin; create takes `{username,email,password,role}` and rejects emails outside the allowed domain |
| PUT | `/api/users/{id}/role` | admin, `{role}` |

Send `Authorization: Bearer <token>` on everything except login. Errors share one JSON shape
(`timestamp, status, error, message, fieldErrors?`).

## Layout
```
backend/src/main/java/com/company/incidentportal
  controller/ service/ repository/ entity/ dto/ security/ exception/ config/
frontend/src
  pages/ components/ services/ context/ utils/
```
Logs go to console and `backend/logs/incident-portal.log` (login attempts, incident creation, status changes, exceptions).
