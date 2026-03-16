package com.st20313779.controller;

import com.st20313779.model.DayPlan;
import com.st20313779.service.PlannerService;
import com.st20313779.service.RecipeService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class PlannerEndpointIT {

    @InjectMock
    PlannerService plannerService;

    @InjectMock
    RecipeService recipeService;

    @Test
    void equivalencePartition_getDayPlan_shouldReturn200_forValidIsoDate() {
        // Arrange
        assertNotNull(recipeService);
        LocalDate date = LocalDate.parse("2026-03-16");
        when(plannerService.getDayPlan(date)).thenReturn(dayPlan(date));

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2026-03-16")
                .then()
                .statusCode(200)
                .body("date", equalTo("2026-03-16"));
    }

    @Test
    void equivalencePartition_getDayPlan_shouldReturn400_forWrongDelimiter() {
        // Arrange

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2026.03.16")
                .then()
                .statusCode(400);
    }

    @Test
    void equivalencePartition_getDayPlan_shouldReturn400_forAlphabeticInput() {
        // Arrange

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "not-a-date")
                .then()
                .statusCode(400);
    }

    @Test
    void equivalencePartition_getDayPlan_shouldReturn400_forCalendarInvalidDate() {
        // Arrange

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2026-02-29")
                .then()
                .statusCode(400);
    }

    @Test
    void equivalencePartition_getDayPlan_shouldReturn400_forDateTimeString() {
        // Arrange

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2026-03-16T10:15:30")
                .then()
                .statusCode(400);
    }

    @Test
    void boundaryValue_getDayPlan_shouldReturn200_forLeapDay() {
        // Arrange
        LocalDate date = LocalDate.parse("2024-02-29");
        when(plannerService.getDayPlan(date)).thenReturn(dayPlan(date));

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2024-02-29")
                .then()
                .statusCode(200)
                .body("date", equalTo("2024-02-29"));
    }

    @Test
    void boundaryValue_getDayPlan_shouldReturn400_forNonLeapYearFeb29() {
        // Arrange

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2023-02-29")
                .then()
                .statusCode(400);
    }

    @Test
    void boundaryValue_getDayPlan_shouldReturn200_forLowerBoundaryDate() {
        // Arrange
        LocalDate date = LocalDate.parse("0001-01-01");
        when(plannerService.getDayPlan(date)).thenReturn(dayPlan(date));

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "0001-01-01")
                .then()
                .statusCode(200)
                .body("date", equalTo("0001-01-01"));
    }

    @Test
    void boundaryValue_getDayPlan_shouldReturn200_forUpperBoundaryDate() {
        // Arrange
        LocalDate date = LocalDate.parse("9999-12-31");
        when(plannerService.getDayPlan(date)).thenReturn(dayPlan(date));

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "9999-12-31")
                .then()
                .statusCode(200)
                .body("date", equalTo("9999-12-31"));
    }

    @Test
    void integrationContract_saveDayPlan_shouldReturn502_whenServiceThrowsRuntimeException() {
        // Arrange
        String payload = """
                {
                  "date": "2026-03-16",
                  "mealUuids": {
                    "BREAKFAST": ["uuid-1"]
                  }
                }
                """;
        when(plannerService.saveDayPlan(any())).thenThrow(new RuntimeException("Recipe service unavailable"));

        // Act + Assert
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/planner/save/day")
                .then()
                .statusCode(502)
                .body("status", equalTo(502))
                .body("error", equalTo("Bad Gateway"))
                .body("message", equalTo("Service layer failure"));
    }

    @Test
    void integrationContract_getDayPlan_shouldReturn502_whenServiceThrowsRuntimeException() {
        // Arrange
        LocalDate date = LocalDate.parse("2026-03-16");
        when(plannerService.getDayPlan(date)).thenThrow(new RuntimeException("Recipe service unavailable"));

        // Act + Assert
        given()
                .when()
                .get("/planner/view/day/{date}", "2026-03-16")
                .then()
                .statusCode(502)
                .body("status", equalTo(502))
                .body("error", equalTo("Bad Gateway"))
                .body("message", equalTo("Service layer failure"));
    }

    private DayPlan dayPlan(final LocalDate date) {
        return new DayPlan(date, new HashMap<>());
    }
}


