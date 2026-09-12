# Plani i implementimit të Backend/API

## Qëllimi
Ky dokument përcakton kontratën fillestare të backend-it për eShkolla dhe shërben si urë mes Web-it, Android-it dhe bazës reale të të dhënave.

## Parimet
- API REST me JSON.
- Autentikim me token/session të sigurt; kredencialet nuk ruhen në klient.
- Autorizim sipas rolit dhe shkollës.
- Validim server-side për çdo kërkesë.
- Soft-delete për entitetet kryesore.
- Audit log për veprimet administrative.
- Pagination, search, filter dhe sort për listat.
- Web dhe Android përdorin të njëjtat kontrata API.

## Entitetet fillestare
- users
- schools
- classes
- students
- teachers
- subjects
- schedules
- grades
- announcements

## Endpoint-et bazë
### Auth
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

### Students
- `GET /api/v1/students`
- `GET /api/v1/students/{id}`
- `POST /api/v1/students`
- `PUT /api/v1/students/{id}`
- `PATCH /api/v1/students/{id}/status`

### Teachers
- `GET /api/v1/teachers`
- `GET /api/v1/teachers/{id}`
- `POST /api/v1/teachers`
- `PUT /api/v1/teachers/{id}`
- `PATCH /api/v1/teachers/{id}/status`

### Classes
- `GET /api/v1/classes`
- `GET /api/v1/classes/{id}`
- `POST /api/v1/classes`
- `PUT /api/v1/classes/{id}`
- `PATCH /api/v1/classes/{id}/status`

## Query standard
Listat duhet të pranojnë një standard të përbashkët:
`page`, `pageSize`, `search`, `sortBy`, `sortDirection`, plus filtra specifikë të entitetit.

Shembull:
`GET /api/v1/students?page=1&pageSize=25&search=Ardit&classId=C1&status=active&sortBy=lastName&sortDirection=asc`

## Përgjigjja standarde
Sukseset e listave duhet të kthejnë metadata pagination dhe rezultatet. Gabimet duhet të kenë kod, mesazh për përdoruesin dhe detaje validimi kur është e nevojshme.

## Siguria
- Password-et ruhen vetëm si hash të fortë.
- Tokenët nuk ekspozohen në logje.
- Kontrolli i rolit bëhet në server, jo vetëm në UI.
- Input-et sanitizohen/validohen.
- Auditimi regjistron përdoruesin, veprimin, entitetin, kohën dhe rezultatin.

## Renditja e realizimit
1. Backend skeleton + konfigurimi.
2. Database schema + migrations.
3. Auth.
4. Students CRUD.
5. Teachers CRUD.
6. Classes CRUD.
7. Subjects/Schedule.
8. Grades.
9. Announcements.
10. Integrimi Web dhe Android.
