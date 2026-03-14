# Weekly Meal Planner — Backend

A Quarkus REST API that stores and retrieves daily meal plans. Each plan maps a date to one or more meal slots (Breakfast, Lunch, Dinner, Other), each containing a list of recipes fetched from an external Recipe API and cached locally.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Quarkus |
| Language | Java 17 |
| Database | PostgreSQL 16 (Docker) |
| Migrations | Flyway |
| External API | Recipe API (MicroProfile REST Client) |
| Utilities | Lombok |

---

## Local Development Setup

### Prerequisites
- Java 21
- Maven
- Docker Desktop

### 1. Start the database

A `docker-compose.yml` is included at the project root. It starts a PostgreSQL 16 container backed by a **named Docker volume** (`meal_planner_data`) so data persists across restarts.

```cmd
docker compose up -d
```

| Setting | Value |
|---|---|
| Host | `localhost:5432` |
| Database | `meal_planner` |
| Username | `planner` |
| Password | `planner` |

Flyway runs automatically on app startup and applies all migrations in `src/main/resources/db/migration/`.

### 2. Run the application

```cmd
./mvnw quarkus:dev
```

### Database lifecycle

```cmd
# Start (data is preserved)
docker compose up -d

# Stop (data is preserved)
docker compose stop

# Wipe all data and start fresh
docker compose down -v
```

---

## Testing

### Prerequisites
- Docker must be running before `mvn test`.
- Tests use Quarkus Dev Services, which starts a PostgreSQL container automatically.


### Run tests

```cmd
mvn test
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/planner/recipes` | Search recipes from the external API |
| `POST` | `/planner/save/day` | Save a meal plan for a given day |
| `GET` | `/planner/view/day/{date}` | Retrieve the meal plan for a given day |

### GET `/planner/recipes`

Searches the external Recipe API. Supports query parameters: `q`, `category`, `cuisine`, `difficulty`, `dietary`, `ingredients`, `page`, `per_page`.

---

### POST `/planner/save/day`

Accepts a `DayMealsDto` body mapping each `MealSlot` to a list of recipe UUIDs. For each UUID the backend fetches the full recipe from the external API, caches it in `public.recipe`, persists the slot–recipe links to `public.meals`, and returns the fully resolved `DayPlan`.

**Request body**
```json
{
  "date": "2026-03-14",
  "mealUuids": {
    "BREAKFAST": ["uuid-1"],
    "LUNCH":     ["uuid-2", "uuid-3"],
    "DINNER":    ["uuid-4"]
  }
}
```

**Response** — `DayPlan` with full recipe objects resolved per slot.

---

### GET `/planner/view/day/{date}`

Returns the `DayPlan` for the given date by loading the meal–slot rows from the database and joining them to the cached recipe records.

**Response**
```json
{
  "date": "2026-03-14",
  "meals": {
    "BREAKFAST": [ { "id": "uuid-1", "name": "Porridge", ... } ],
    "LUNCH":     [ { ... }, { ... } ]
  }
}
```

---

## Acceptance Criteria

### Save Day Plan — `POST /planner/save/day`

**AC-1 — Happy path**
> **Given** a valid request body containing a date and at least one slot with one or more recipe UUIDs,
> **When** `POST /planner/save/day` is called,
> **Then** each UUID is looked up via the external Recipe API, the recipe is saved to the database, the slot–UUID links are persisted to the `meals` table, and the fully resolved `DayPlan` is returned with HTTP 200.

**AC-2 — Unrecognised meal slot**
> **Given** a request body containing a slot value that does not match `BREAKFAST`, `LUNCH`, `DINNER`, or `OTHER`,
> **When** `POST /planner/save/day` is called,
> **Then** the request is rejected with HTTP 400 before any external API calls are made.

**AC-3 — Recipe UUID not found in external API**
> **Given** a request body containing a recipe UUID that the external Recipe API cannot find,
> **When** `POST /planner/save/day` is called,
> **Then** the request fails with HTTP 502 (or appropriate upstream error) and no partial data is persisted.

**AC-4 — Missing or malformed date**
> **Given** a request body where the `date` field is absent or not a valid ISO-8601 date,
> **When** `POST /planner/save/day` is called,
> **Then** the request is rejected with HTTP 400.

---

### View Day Plan — `GET /planner/view/day/{date}`

**AC-5 — Happy path**
> **Given** the `meals` table contains one or more entries for the requested date,
> **When** `GET /planner/view/day/{date}` is called with a valid ISO-8601 date,
> **Then** a `DayPlan` is returned with each slot populated by its resolved `Recipe` objects and HTTP 200.

**AC-6 — Date with no saved plan**
> **Given** the `meals` table contains no entries for the requested date,
> **When** `GET /planner/view/day/{date}` is called,
> **Then** an empty `DayPlan` (no slots) is returned with HTTP 200.

**AC-7 — Invalid date format**
> **Given** the `{date}` path parameter is not a valid ISO-8601 date (e.g. `"tomorrow"` or `"14-03-2026"`),
> **When** `GET /planner/view/day/{date}` is called,
> **Then** the request is rejected with HTTP 400.

---

## Architecture

A sequence diagram showing the end-to-end flow for both operations is available at [`docs/sequence-diagram.puml`](docs/sequence-diagram.puml).


