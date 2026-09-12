# eShkolla Backend

Backend-i i eShkolla po ndërtohet si shërbim REST me Kotlin + Ktor.

## Gjendja aktuale

- Ktor service skeleton aktiv
- `GET /health`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- CRUD bazë për nxënësit
- Search, filter aktiv/inaktiv dhe pagination për nxënësit
- DTO të përbashkëta për kontratën API
- Validim bazë server-side
- Soft delete për nxënësit
- PBKDF2 për hashimin e fjalëkalimeve demo
- Teste automatike për health, login dhe listimin e nxënësve

## Arkitektura

```text
Web ───────┐
           ├── REST API ── Service ── Repository ── Database
Android ───┘
```

Klientët nuk do të kenë qasje direkte në databazë. API është burimi qendror i të dhënave.

## Konfigurimi lokal

```bash
gradle :backend:run
```

API nis në portin `8080` dhe health-check është:

```text
GET http://localhost:8080/health
```

Përdoruesit demo të fazës së zhvillimit përdorin fjalëkalimin `123456`. Këto kredenciale nuk duhet të përdoren në prodhim.

## Hapat pasues

1. Migrimet dhe PostgreSQL.
2. Repository real me databazë.
3. Role/permissions server-side për çdo modul.
4. Teachers, classes, subjects, schedule, grades dhe announcements.
5. OpenAPI/Swagger.
6. Integrimi Web dhe Android me API.
7. Audit log dhe testet e sigurisë.
