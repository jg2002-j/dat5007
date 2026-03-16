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

## Architecture

A sequence diagram showing the end-to-end flow for both operations is available at [`docs/sequence-diagram.puml`](docs/sequence-diagram.puml).


