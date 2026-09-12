# eShkolla Backend

Shtresa backend do të jetë burimi qendror i të dhënave për Web dhe Android.

## Arkitektura e synuar

```text
Web ───────┐
           ├── REST API ── Service ── Repository ── Database
Android ───┘
```

## Faza aktuale
Ky folder përgatit kontratën dhe strukturën për backend-in real. Të dhënat demo në Web/Android mbeten të izoluara derisa API dhe database të jenë gati.

## Kërkesa minimale
- konfigurim sipas environment-it
- migrations të versionuara
- autentikim dhe autorizim
- validim i input-it
- logging dhe audit
- health check
- dokumentim OpenAPI
- teste automatike

## Rregull
Asnjë klient nuk duhet të lidhet direkt me database. Web dhe Android komunikojnë vetëm përmes API-së.
