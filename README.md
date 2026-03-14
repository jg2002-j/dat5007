# Weekly Meal Planner — Backend

A Quarkus REST API that stores and retrieves daily meal plans. Each plan maps a date to one or more meal slots (Breakfast, Lunch, Dinner, Other), each containing a list of recipes fetched from an external Recipe API and cached locally.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Quarkus |
| Language | Java 17 |
| Database | PostgreSQL |
| Migrations | Flyway |
| External API | Recipe API (MicroProfile REST Client) |
| Utilities | Lombok |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/planner/` | Save a meal plan for a given day |
| `GET` | `/planner/day/{date}` | Retrieve the meal plan for a given day |

### POST `/planner/`

Accepts a `DayMealsDto` body mapping each `MealSlot` to a list of recipe UUIDs. For each UUID the backend fetches the full recipe from the external API, caches it in the database, then persists the slot–recipe links.

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

**Response** — `DayPlan` with recipes resolved per slot.

---

### GET `/planner/day/{date}`

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

### Save Day Plan — `POST /planner/`

**AC-1 — Happy path**
> **Given** a valid request body containing a date and at least one slot with one or more recipe UUIDs,
> **When** `POST /planner/` is called,
> **Then** each UUID is looked up via the external Recipe API, the recipe is saved to the database, the slot–UUID links are persisted to the `meals` table, and the fully resolved `DayPlan` is returned with HTTP 200.

**AC-2 — Unrecognised meal slot**
> **Given** a request body containing a slot value that does not match `BREAKFAST`, `LUNCH`, `DINNER`, or `OTHER`,
> **When** `POST /planner/` is called,
> **Then** the request is rejected with HTTP 400 before any external API calls are made.

**AC-3 — Recipe UUID not found in external API**
> **Given** a request body containing a recipe UUID that the external Recipe API cannot find,
> **When** `POST /planner/` is called,
> **Then** the request fails with HTTP 502 (or appropriate upstream error) and no partial data is persisted.

**AC-4 — Missing or malformed date**
> **Given** a request body where the `date` field is absent or not a valid ISO-8601 date,
> **When** `POST /planner/` is called,
> **Then** the request is rejected with HTTP 400.

---

### View Day Plan — `GET /planner/day/{date}`

**AC-5 — Happy path**
> **Given** the `meals` table contains one or more entries for the requested date,
> **When** `GET /planner/day/{date}` is called with a valid ISO-8601 date,
> **Then** a `DayPlan` is returned with each slot populated by its resolved `Recipe` objects and HTTP 200.

**AC-6 — Date with no saved plan**
> **Given** the `meals` table contains no entries for the requested date,
> **When** `GET /planner/day/{date}` is called,
> **Then** an empty `DayPlan` (no slots) is returned with HTTP 200.

**AC-7 — Invalid date format**
> **Given** the `{date}` path parameter is not a valid ISO-8601 date (e.g. `"tomorrow"` or `"14-03-2026"`),
> **When** `GET /planner/day/{date}` is called,
> **Then** the request is rejected with HTTP 400.

---

## Architecture

A sequence diagram showing the end-to-end flow for both operations is available at [`docs/sequence-diagram.puml`](docs/sequence-diagram.puml).
