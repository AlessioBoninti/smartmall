# SmartMall: note rapide

La documentazione operativa del progetto è nel file `README.md`.

Riferimenti utili per lo stack usato:

- Spring Boot: https://docs.spring.io/spring-boot/3.5.11/reference/
- Spring Security: https://docs.spring.io/spring-security/reference/
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/reference/
- Vite: https://vite.dev/guide/
- React: https://react.dev/learn

Per verificare il progetto prima di condividerlo:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
cd frontend
npm install
npm audit
npm run build
```

