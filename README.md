# SmartMall

SmartMall è una web app responsive per gestire prenotazioni negli store di un centro commerciale. Include autenticazione, ruoli, gestione store, disponibilità orarie e prenotazioni.

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
- Sicurezza: Spring Security, Firebase Authentication, autorizzazione basata su ruoli.
- Database: MySQL 8.
- Persistenza: Spring Data JPA, Hibernate.
- Build: Maven per backend, npm/Vite per frontend.
- Docker: Docker Compose per avviare MySQL, backend e frontend.

## Prerequisiti

- Java 17.
- Node.js e npm.
- Docker Desktop o Docker Engine.

## Configurazione locale

Il progetto viene eseguito con configurazione locale/dev. Il profilo attivo è `dev`, definito in `src/main/resources/application.properties`, e abilita il `DataSeeder` per creare i dati demo se mancanti.

Per il backend servono queste variabili d'ambiente:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
$env:FIREBASE_PROJECT_ID="your-project-id"
$env:FIREBASE_SERVICE_ACCOUNT_JSON="..."
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:4173"
$env:ENABLE_DEMO_DATA="true"
```

Per il frontend, opzionale:

```powershell
$env:VITE_API_URL="http://localhost:8080"
$env:VITE_FIREBASE_API_KEY="your-api-key"
$env:VITE_FIREBASE_AUTH_DOMAIN="your-project.firebaseapp.com"
$env:VITE_FIREBASE_PROJECT_ID="your-project-id"
$env:VITE_FIREBASE_APP_ID="your-app-id"
```

Il file `.env.example` contiene un modello con placeholder sicuri. Non committare file `.env` reali: sono giÃ  esclusi da `.gitignore`.

`CORS_ALLOWED_ORIGINS` accetta piÃ¹ origin separati da virgola. In locale il default resta:

```text
http://localhost:5173,http://localhost:4173
```

`ENABLE_DEMO_DATA` controlla il caricamento dei dati demo:

```text
ENABLE_DEMO_DATA=true
```

mantiene customer, merchant, admin e store demo. In cloud si puÃ² impostare:

```text
ENABLE_DEMO_DATA=false
```

per evitare la creazione automatica dei dati demo.

## Avvio con Docker Compose

Docker Compose avvia MySQL, backend e frontend.

```powershell
docker compose up --build
```

Il database locale sarà disponibile su:

```text
host: localhost
porta: 3306
database: smartmall
user: root
password: root
```

`root` Ã¨ solo un default locale per semplificare la demo. Per un deploy online impostare `DB_USERNAME` e `DB_PASSWORD` con valori dedicati tramite variabili d'ambiente o secret del provider.

## Avvio backend Spring Boot

Dalla cartella principale del progetto:

```powershell
$env:DB_PASSWORD="root"
$env:FIREBASE_PROJECT_ID="your-project-id"
$env:FIREBASE_SERVICE_ACCOUNT_JSON="..."
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:4173"
$env:ENABLE_DEMO_DATA="true"
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

Aprire l'applicazione da:

```text
http://localhost:5173
```

Nota CORS: usare `http://localhost:5173`, non `http://127.0.0.1:5173`. Il backend autorizza esplicitamente `localhost:5173` e `localhost:4173`.

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

## CI/CD

La pipeline GitHub Actions è definita in `.github/workflows/ci.yml`.

Su pull request e push su `main` esegue:

- test backend con Maven;
- build del jar backend;
- installazione e build del frontend.

Su push su `main` e avvio manuale pubblica anche le immagini Docker su GitHub Container Registry.

Il deploy Kubernetes è manuale tramite `workflow_dispatch`, impostando l'input `deploy` a `true`.

Variabili GitHub Actions richieste per il frontend:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_APP_ID`

Variabili GitHub Actions per la configurazione runtime:

- `DB_URL`
- `DB_USERNAME`
- `HIBERNATE_DDL_AUTO`
- `SPRING_JPA_SHOW_SQL`
- `SPRING_PROFILES_ACTIVE`
- `FIREBASE_PROJECT_ID`
- `CORS_ALLOWED_ORIGINS`
- `ENABLE_DEMO_DATA`
- `GHCR_PULL_USERNAME` se le immagini GHCR sono private

Secret GitHub Actions richiesti per il deploy:

- `KUBE_CONFIG_DATA`: kubeconfig del cluster codificato in Base64;
- `DB_PASSWORD`;
- `FIREBASE_SERVICE_ACCOUNT_JSON`;
- `GHCR_PULL_TOKEN` se le immagini GHCR sono private.

Il deploy applica i manifest Kubernetes, sostituisce le immagini locali con quelle pubblicate su GHCR e aggiorna ConfigMap e Secret direttamente nel cluster.

## Info utili

- Il progetto usa una configurazione locale, non una configurazione di produzione.
- Il database viene aggiornato automaticamente in sviluppo con `spring.jpa.hibernate.ddl-auto=update`.
- Il progetto usa dati demo generati dal `DataSeeder`: tre utenti, uno store demo e una disponibilità iniziale per il sabato.
- `DB_PASSWORD`, `FIREBASE_PROJECT_ID` e `FIREBASE_SERVICE_ACCOUNT_JSON` devono essere impostate prima di avviare il backend.
- `CORS_ALLOWED_ORIGINS` permette di aggiungere l'URL pubblico del frontend senza modificare il codice.
- `ENABLE_DEMO_DATA=false` evita la creazione automatica dei dati demo in cloud.
- `/actuator/health` espone un health check semplice del backend.
- Non è presente un'app mobile nativa: l'interfaccia è una web app responsive.
- La sospensione store è manuale e immediata: lo store resta non prenotabile finché l'admin non lo riattiva.
- L'approvazione di una richiesta merchant cambia solo il ruolo utente; l'assegnazione di store non è automatica in questa demo.
