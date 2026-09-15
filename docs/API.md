# eShkolla API — përmbledhje

Base URL në zhvillim: `http://localhost:8080/api/v1`

Të gjitha endpoint-et private kërkojnë:

`Authorization: Bearer <token>`

## Orari

- `GET /schedule` — lexon orarin.
- `POST /schedule` — Administrator/Drejtor.
- `PUT /schedule/{id}` — Administrator/Drejtor.
- `DELETE /schedule/{id}` — Administrator/Drejtor.

Shembull:

```json
{
  "classId": "C03",
  "teacherId": "M001",
  "subjectId": "MAT",
  "weekday": 1,
  "startTime": "08:00",
  "endTime": "08:45",
  "room": "Kabineti 3",
  "active": true
}
```

## Njoftimet

- `GET /notifications`
- `POST /notifications` — Administrator/Drejtor.

Shembull:

```json
{
  "title": "Takimi me prindër",
  "message": "Takimi mbahet të premten në ora 13:00.",
  "audience": "ALL"
}
```

## Raportet

`GET /reports/academic`

Për nxënësin përdoret `studentId` vetëm kur përdoruesi ka të drejtë ta shohë atë nxënës. Për rolin Nxënës përdoret automatikisht profili i vet; për Prindin lejohet vetëm fëmija i lidhur.

Rezultati përmban:
- numrin e notave;
- mesataren;
- numrin total të mungesave;
- mungesat e arsyeshme;
- mungesat e paarsyeshme.

## Auth

- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `GET /me/profile`
- `GET /me/permissions`
- `GET /dashboard`
- `GET /me/student`
- `GET /me/children`
- `GET /me/teacher`

## Parimi

Validimi dhe autorizimi bëhen në backend. Android dhe Web nuk duhet të konsiderohen burim autoriteti për të drejtat e përdoruesit.
