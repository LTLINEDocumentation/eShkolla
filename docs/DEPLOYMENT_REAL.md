# eShkolla – aktivizimi me të dhëna reale

## Arkitektura
- Android + Web: klientët
- Ktor: API
- PostgreSQL i menaxhuar: databaza
- HTTPS: komunikimi i enkriptuar
- GitHub: kodi

## Testim fillestar
1. Krijo databazën PostgreSQL në Neon.
2. Krijo Web Service në Render nga repository `LTLINEDocumentation/eShkolla`.
3. Render përdor `Dockerfile` në root dhe `render.yaml`.
4. Në Render vendos vetëm secrets në Environment:
   - `DATABASE_URL` = connection string PostgreSQL
   - `BOOTSTRAP_ADMIN_USERNAME` = emri i administratorit
   - `BOOTSTRAP_ADMIN_PASSWORD` = fjalëkalim i fortë, së paku 10 karaktere
   - `BOOTSTRAP_ADMIN_NAME` = emri i administratorit
5. Pas deploy kontrollo `https://ADRESA-E-RENDER/health`.
6. Vetëm pasi `/health` të përgjigjet, lidhet Android me URL-në e API-së.

## Siguria
- Asnjë password i databazës nuk vendoset në GitHub ose në APK.
- `DATABASE_URL` ruhet si secret në Render.
- PostgreSQL përdor SSL (`sslmode=require` nëse mungon nga URL-ja).
- Password-et e përdoruesve ruhen me PBKDF2-HMAC-SHA256 dhe salt unik.
- Llogaritë kontrollohen sipas rolit dhe statusit aktiv.
- Para përdorimit me të dhëna reale shkollore duhen shtuar: skadim automatik i sesioneve, rate limiting, audit log, backup/restore dhe politikë për ruajtjen/fshirjen e të dhënave.

## Të dhënat reale
Mos futim menjëherë të dhëna të nxënësve në serverin falas. Fillimisht bëjmë një pilot me të dhëna testuese. Pastaj importojmë:
- shkollën
- klasat
- mësimdhënësit
- nxënësit
- prindërit
- lëndët
- orarin
- notat
- mungesat
- njoftimet

Importi duhet të bëhet me lidhje të kontrolluara dhe pa ekspozuar fjalëkalimet.
