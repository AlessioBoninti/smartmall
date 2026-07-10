# SmartMall

SmartMall è una web app responsive per gestire prenotazioni negli store di un centro commerciale. Il progetto è pensato come demo universitaria locale: mostra autenticazione, ruoli, gestione store, disponibilità orarie e prenotazioni.

## Funzionalità principali

- Registrazione e login utenti.
- Consultazione store e slot disponibili anche da visitatore.
- Prenotazione slot da parte dei customer.
- Cancellazione prenotazioni con regole di anticipo.
- Richiesta per diventare merchant: l'approvazione cambia il ruolo, ma non crea automaticamente uno store.
- Gestione store, disponibilità e prenotazioni lato merchant.
- Gestione utenti, store, richieste ruolo e consultazione prenotazioni lato admin.

## Stack tecnico

- Frontend: React 18, Vite, JavaScript, CSS, lucide-react.
- Backend: Java 17, Spring Boot 3.5, Spring Web, Spring Validation.
- Sicurezza: Spring Security, JWT.
- Database: MySQL 8.
- Persistenza: Spring Data JPA, Hibernate.
- Build: Maven per backend, npm/Vite per frontend.
- Docker: Docker Compose usato solo per avviare MySQL.

## Prerequisiti

- Java 17.
- Node.js e npm.
- Docker Desktop o Docker Engine.

## Configurazione locale

Il progetto viene eseguito con configurazione locale/dev. Il profilo attivo è `dev`, definito in `src/main/resources/application.properties`, e abilita il `DataSeeder` per creare i dati demo se mancanti.

Per il backend servono queste variabili d'ambiente:

```powershell
$env:DB_PASSWORD="root"
$env:JWT_SECRET="01234567890123456789012345678901"
```

Per il frontend, opzionale:

```powershell
$env:VITE_API_URL="http://localhost:8080"
```

## Avvio MySQL con Docker

Docker Compose avvia solo MySQL, non backend o frontend.

```powershell
docker compose up -d
```

Il database locale sarà disponibile su:

```text
host: localhost
porta: 3306
database: smartmall
user: root
password: root
```

## Avvio backend Spring Boot

Dalla cartella principale del progetto:

```powershell
$env:DB_PASSWORD="root"
$env:JWT_SECRET="01234567890123456789012345678901"
.\mvnw.cmd spring-boot:run
```

Il backend parte su:

```text
http://localhost:8080
```

## Avvio frontend React/Vite

Da una seconda shell:

```powershell
cd frontend
npm install
npm run dev
```

Aprire la demo da:

```text
http://localhost:5173
```

Nota CORS: usare `http://localhost:5173`, non `http://127.0.0.1:5173`. Il backend autorizza esplicitamente solo `http://localhost:5173`.

## Credenziali demo

| Ruolo | Email | Password |
| --- | --- | --- |
| Customer | `customer@test.com` | `password123` |
| Merchant | `merchant@test.com` | `password123` |
| Super admin | `admin@test.com` | `password123` |

## Comandi build e test

Backend:

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm install
npm run build
```

## Flusso demo consigliato

1. Avviare MySQL con Docker Compose.
2. Avviare backend e frontend separatamente.
3. Login come customer e prenotazione di uno slot. I dati demo creano disponibilità predefinita il sabato, quindi scegliere un sabato futuro.
4. Cancellazione di una prenotazione con sufficiente anticipo.
5. Richiesta per diventare merchant.
6. Login come merchant demo e gestione disponibilità dello store già assegnato.
7. Login come admin e gestione utenti, store e richieste ruolo.
8. Spiegazione rapida di JWT, ruoli e protezione delle API.

## Info utili

- Il progetto è una demo universitaria locale, non una configurazione di produzione.
- Backend e frontend si avviano separatamente.
- Il database viene aggiornato automaticamente in sviluppo con `spring.jpa.hibernate.ddl-auto=update`.
- Il progetto usa dati demo generati dal `DataSeeder`: tre utenti, uno store demo e una disponibilità iniziale per il sabato.
- `DB_PASSWORD` e `JWT_SECRET` devono essere impostate come variabili d'ambiente prima di avviare il backend.
- Non è presente un'app mobile nativa: l'interfaccia è una web app responsive.
- La sospensione store è manuale e immediata: lo store resta non prenotabile finché l'admin non lo riattiva.
- L'approvazione di una richiesta merchant cambia solo il ruolo utente; l'assegnazione di store non è automatica in questa demo.
