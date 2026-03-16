package com.st20313779.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class PlannerEndpointIT {

    @Test
    void saveDayPlan_returns200_forValidRequest() {
        final String payload = """
                {
                  "date": "2026-03-14",
                  "mealUuids": {}
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/planner/save/day")
                .then()
                .statusCode(200);
    }

    @Test
    void saveDayPlan_rejectsUnrecognisedMealSlot_with400() {
        final String payload = """
                {
                  "date": "2026-03-14",
                  "mealUuids": {
                    "BRUNCH": ["e8ec8e7f-8fa7-4b44-9647-b0fc82a14fa5"]
                  }
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/planner/save/day")
                .then()
                .statusCode(400);
    }

    @Test
    void saveDayPlan_acceptsBoundaryDate_farPast() {
        final String payload = """
                {
                  "date": "1900-01-01",
                  "mealUuids": {}
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/planner/save/day")
                .then()
                .statusCode(200);
    }

    @Test
    void saveDayPlan_acceptsBoundaryDate_farFuture() {
        final String payload = """
                {
                  "date": "2099-12-31",
                  "mealUuids": {}
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/planner/save/day")
                .then()
                .statusCode(200);
    }
}

