# eShkolla

Platformë digjitale për menaxhimin dhe organizimin e proceseve shkollore.

## Gjendja aktuale

Projekti ka një Web demo të publikuar në GitHub Pages dhe një aplikacion Android me Kotlin + Jetpack Compose. Autentikimi aktual është demo; API dhe baza reale do të ndërtohen në fazën pasuese.

## Struktura

```text
.
├── app/                         # Android / Kotlin / Compose
│   └── src/main/java/com/ltline/eshkolla/
│       ├── app/
│       ├── data/
│       ├── domain/
│       ├── features/
│       └── ui/
├── web/                         # Web demo për testim të vazhdueshëm
│   ├── index.html
│   ├── app.js
│   └── styles.css
├── docs/
│   ├── api/
│   ├── architecture/
│   ├── database/
│   └── requirements/
└── README.md
```

## Rolet

- Administrator
- Drejtor
- Mësimdhënës
- Nxënës
- Prind

## Parimi i zhvillimit

Çdo funksion i ri zhvillohet dhe kontrollohet fillimisht në Web, pastaj integrohet në Android dhe më vonë lidhet me API-në dhe bazën reale.

`Domain → Data demo → Web test → Android → API → Database → Testim`

## Web

Demo: https://ltlinedocumentation.github.io/eShkolla/

## Siguria

Kredencialet e demo-s janë vetëm për zhvillim. Para përdorimit real duhet autentikim server-side, ruajtje e sigurt e fjalëkalimeve, token-e, autorizim sipas rolit, auditim dhe validim në backend.
