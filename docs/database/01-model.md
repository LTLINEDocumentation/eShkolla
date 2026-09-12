# Modeli fillestar i të dhënave

## Entitetet kryesore

- `School` – institucioni shkollor.
- `User` – llogaria e përdoruesit.
- `SchoolClass` – klasa/paralela dhe viti shkollor.
- `Student` – nxënësi dhe lidhja me klasën.
- `Teacher` – mësimdhënësi dhe lidhja me përdoruesin.
- `Subject` – lënda mësimore.
- `ScheduleEntry` – një orë në orar.
- `Grade` – vlerësimi i nxënësit.
- `Announcement` – njoftimi i shkollës.

## Marrëdhëniet

`School 1—N SchoolClass`

`SchoolClass 1—N Student`

`School 1—N Teacher`

`School 1—N Subject`

`Student 1—N Grade`

`Subject 1—N Grade`

`Teacher 1—N Grade`

`SchoolClass 1—N ScheduleEntry`

`Subject 1—N ScheduleEntry`

`Teacher 1—N ScheduleEntry`

## Rregulla

- ID-të janë unike.
- Nota lejohet vetëm nga 1 deri në 5.
- Klasa lidhet me një vit shkollor.
- Nxënësi aktiv duhet të ketë klasë aktive.
- Autorizimi i përdoruesve do të kontrollohet në backend.
