# Cypress E2E Tests

This project includes API-level end-to-end tests in `cypress/e2e/planner-api.cy.js`.

## Coverage

- `POST /planner/save/day` with empty plan
- save-then-view flow (`POST /planner/save/day` then `GET /planner/view/day/{date}`)
- error handling for null date (`500`)
- invalid date format handling (`400`)

## Run

Start the backend first (test profile recommended so it uses H2):

```powershell
Set-Location "C:\Users\st20313779\IdeaProjects\dat5007"
.\mvnw.cmd quarkus:dev "-Dquarkus.profile=test" "-Dquarkus.http.port=8081"
```

In a second terminal, run Cypress:

```powershell
Set-Location "C:\Users\st20313779\IdeaProjects\dat5007"
npm run cypress:run:planner
```

Or run all Cypress specs:

```powershell
Set-Location "C:\Users\st20313779\IdeaProjects\dat5007"
npm run cypress:run
```

