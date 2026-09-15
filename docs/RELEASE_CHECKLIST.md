# eShkolla — Release Checklist

## 1–3. Baza e sistemit
- [x] Autentikim server-side me PBKDF2
- [x] Role: Administrator, Drejtor, Mësimdhënës, Nxënës, Prind
- [x] PostgreSQL + HikariCP
- [x] Nxënës, klasa, mësimdhënës dhe caktime
- [x] Notat CRUD
- [x] Mungesat CRUD

## 4. Orari
- [x] Tabela `schedules`
- [x] GET/POST/PUT/DELETE `/api/v1/schedule`
- [x] Shfaqje e filtruar për mësimdhënësin
- [ ] UI Android për administrimin e orarit
- [ ] UI Web e lidhur me API

## 5. Njoftimet
- [x] Tabela `notifications`
- [x] GET/POST `/api/v1/notifications`
- [x] Audienca e njoftimit
- [ ] Push notifications reale (FCM)
- [ ] UI Android/Web e lidhur me API

## 6. Raportet
- [x] `/api/v1/reports/academic`
- [x] Mesatarja e notave
- [x] Numri i mungesave
- [x] Ndarja arsyeshme/paarsyeshme
- [ ] Eksport PDF/Excel

## 7. Nxënësi
- [x] Lidhje `student_users`
- [x] `/api/v1/me/student`
- [x] Dashboard me module sipas rolit
- [ ] Testim final në pajisje reale

## 8. Prindi
- [x] Lidhje `parent_students`
- [x] `/api/v1/me/children`
- [x] Kufizim i raportit akademik te fëmija i lidhur
- [ ] Testim final me llogari reale

## 9. Siguria
- [x] Fjalëkalime të hash-uara PBKDF2
- [x] Session token server-side
- [x] Kontroll i roleve në backend
- [x] Validim për notat, mungesat dhe orarin
- [ ] HTTPS/TLS në hosting final
- [ ] Secrets jashtë repository-t
- [ ] Rate limiting / audit log
- [ ] Ndryshim i kredencialeve demo para prodhimit

## 10. Testimi
- [x] CI për Android debug APK
- [x] CI për backend
- [ ] Teste automatike për API-të e reja
- [ ] Testim Android në pajisje reale
- [ ] Testim Web në Chrome/Edge/Firefox
- [ ] Testim role-by-role me të dhëna reale

## 11. Release
- [x] Dokumentim bazë i arkitekturës dhe API-ve
- [x] Android debug APK workflow
- [ ] Android release keystore
- [ ] Version final i Play Store
- [ ] Domain/HTTPS për backend
- [ ] PostgreSQL production backup
- [ ] Procedurë backup/restore
- [ ] Udhëzues administratori, mësimdhënësi, nxënësi dhe prindi

> Ky dokument dallon qartë funksionet e implementuara nga ato që kërkojnë konfigurim të infrastrukturës reale para prodhimit.
