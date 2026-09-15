# eShkolla

Platformë digjitale për menaxhimin dhe organizimin e proceseve shkollore.

## Arkitektura

- **Android:** Kotlin + Jetpack Compose
- **Backend:** Kotlin + Ktor 3.0.2
- **Database:** PostgreSQL + HikariCP
- **Web:** GitHub Pages/demo dhe shtresë për integrim me API
- **CI:** GitHub Actions për Android dhe Backend

## Rolet

- Administrator
- Drejtor
- Mësimdhënës
- Nxënës
- Prind

## Funksionet e implementuara

- Autentikim server-side me PBKDF2 dhe session token
- Dashboard sipas rolit
- Menaxhim mësimdhënësish, klasash dhe nxënësish
- Caktim mësimdhënës ↔ klasë
- Nota: krijim, lexim, ndryshim dhe fshirje
- Mungesa: krijim, lexim, ndryshim dhe fshirje
- Orari: model, PostgreSQL dhe API CRUD
- Njoftimet: PostgreSQL dhe API
- Raportet akademike: mesatare, nota dhe mungesa
- Lidhje nxënësi dhe prindi me profilet përkatëse

## Endpoint-et kryesore

- `/api/v1/auth/*`
- `/api/v1/me/*`
- `/api/v1/dashboard`
- `/api/v1/management/*`
- `/api/v1/grades`
- `/api/v1/absences`
- `/api/v1/schedule`
- `/api/v1/notifications`
- `/api/v1/reports/academic`

Dokumentimi: `docs/API.md`, `docs/USER_GUIDE.md`, `docs/RELEASE_CHECKLIST.md`.

## CI / Release

GitHub Actions ndërton Android debug APK dhe backend-in. Para prodhimit duhet të konfigurohen HTTPS, secrets, PostgreSQL production, backup/restore dhe Android release signing.

## Web

Demo: https://ltlinedocumentation.github.io/eShkolla/

## Siguria

Kredencialet demo janë vetëm për zhvillim. Backend-i kontrollon autentikimin, rolet dhe validimin. Para përdorimit real duhet të ndryshohen kredencialet demo dhe të vendosen HTTPS, secrets jashtë repository-t, rate limiting dhe auditim.
