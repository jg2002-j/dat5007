package com.st20313779.repository;

import com.st20313779.model.MealSlot;
import com.st20313779.repository.dto.DayMealsDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PlannerRepoTest {

    @Inject
    PlannerRepo plannerRepo;

    @Inject
    DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("TRUNCATE TABLE public.meals, public.recipe RESTART IDENTITY CASCADE")) {
            ps.executeUpdate();
        }
    }

    @Test
    void shouldPersistMealRowsAndGroupBySlotWhenReadingDayPlan() throws SQLException {
        // Arrange
        LocalDate day = LocalDate.of(2026, 3, 16);
        Date sqlDate = Date.valueOf(day);
        insertRecipe("recipe-1");
        insertRecipe("recipe-2");
        insertRecipe("recipe-3");

        // Act
        plannerRepo.saveSlotMealUuid(sqlDate, MealSlot.BREAKFAST, "recipe-1");
        plannerRepo.saveSlotMealUuid(sqlDate, MealSlot.BREAKFAST, "recipe-2");
        plannerRepo.saveSlotMealUuid(sqlDate, MealSlot.DINNER, "recipe-3");
        DayMealsDto result = plannerRepo.getDayPlan(sqlDate);

        // Assert
        assertNotNull(result);
        assertEquals(sqlDate, result.getDate());
        assertEquals(2, result.getMealUuids().size());
        assertEquals(2, result.getMealUuids().get(MealSlot.BREAKFAST).size());
        assertEquals("recipe-1", result.getMealUuids().get(MealSlot.BREAKFAST).get(0));
        assertEquals("recipe-2", result.getMealUuids().get(MealSlot.BREAKFAST).get(1));
        assertEquals(1, result.getMealUuids().get(MealSlot.DINNER).size());
        assertEquals("recipe-3", result.getMealUuids().get(MealSlot.DINNER).get(0));
    }

    @Test
    void shouldReturnEmptyMealMapWhenNoRowsExistForDate() {
        // Arrange
        Date sqlDate = Date.valueOf(LocalDate.of(2030, 1, 1));

        // Act
        DayMealsDto result = plannerRepo.getDayPlan(sqlDate);

        // Assert
        assertNotNull(result);
        assertEquals(sqlDate, result.getDate());
        assertNotNull(result.getMealUuids());
        assertTrue(result.getMealUuids().isEmpty());
    }

    private void insertRecipe(final String recipeId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO public.recipe (recipe_id, name, description, category, cuisine, difficulty, cultural_context) VALUES (?, ?, ?, ?, ?, ?, ?)")
        ) {
            ps.setString(1, recipeId);
            ps.setString(2, "name-" + recipeId);
            ps.setString(3, "description");
            ps.setString(4, "category");
            ps.setString(5, "cuisine");
            ps.setString(6, "easy");
            ps.setString(7, "context");
            ps.executeUpdate();
        }
    }
}

