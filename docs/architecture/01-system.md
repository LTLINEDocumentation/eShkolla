# Arkitektura e sistemit

## Shtresat

1. **UI** – Jetpack Compose në Android dhe Web UI.
2. **Features** – Login, Dashboard dhe modulet e shkollës.
3. **Domain** – modelet dhe kontratat e repository-ve.
4. **Data** – repository demo tani; API/baza reale në fazën pasuese.

## Rregulli i zhvillimit

Çdo funksion i ri:

`Domain → Data demo → Web test → Android → API reale → Testim`

## Role

- ADMINISTRATOR
- DREJTOR
- MESIMDHENES
- NXENES
- PRIND

UI-ja duhet të kufizojë modulet sipas rolit. Kontrolli real i autorizimit do të zbatohet në backend dhe nuk do të mbështetet vetëm në klient.
