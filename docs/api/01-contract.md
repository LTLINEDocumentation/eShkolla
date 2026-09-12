# Kontrata fillestare e API-së

## Autentikimi

`POST /api/v1/auth/login`

Request:

```json
{"username":"admin","password":"..."}
```

Response e planifikuar:

```json
{"accessToken":"...","user":{"id":"1","username":"admin","role":"ADMINISTRATOR"}}
```

## Modulet

- `GET /api/v1/schools`
- `GET /api/v1/classes`
- `GET /api/v1/students`
- `GET /api/v1/teachers`
- `GET /api/v1/subjects`
- `GET /api/v1/schedule`
- `GET /api/v1/grades`
- `GET /api/v1/announcements`

Operacionet administrative do të shtojnë `POST`, `PUT/PATCH` dhe `DELETE` sipas autorizimit.

## Parim sigurie

Klienti nuk konsiderohet burim autoriteti. Çdo endpoint duhet të verifikojë token-in, rolin dhe lejet në server.
