package com.st20313779.repository;

import com.st20313779.fixtures.TestRecipeBuilder;
import com.st20313779.model.recipe.Recipe;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for RecipeRepo class.
 *
 * =========================================================================
 * PHASE 3: INTEGRATION TESTING - Bottom-Up Approach
 * =========================================================================
 *
 * Scope: Test RecipeRepo against real PostgreSQL via Quarkus Dev Services.
 * No mocking - uses actual database transactions.
 *
 * Decision Points Tested:
 * D1: saveRecipe with valid recipe → persisted to DB
 * D2: getRecipeById with valid UUID → found in DB
 * D3: getRecipeById with non-existent UUID → not found
 * D4: getRecipesByIds with multiple UUIDs → correct order preserved
 *
 * RTM MAPPING:
 * TR-015: Persist recipe to database successfully
 * TR-016: Retrieve single recipe by ID
 * TR-017: Retrieve multiple recipes by IDs with order preservation
 * TR-018: Handle non-existent UUID gracefully
 * =========================================================================
 */
@QuarkusTest
@DisplayName("RecipeRepo Integration Tests - Database Persistence")
public class RecipeRepoIT {

    @Inject
    RecipeRepo recipeRepo;

    private String testUuid1;
    private String testUuid2;
    private String testUuid3;

    @BeforeEach
    void setUp() {
        testUuid1 = UUID.randomUUID().toString();
        testUuid2 = UUID.randomUUID().toString();
        testUuid3 = UUID.randomUUID().toString();
    }

    // =========================================================================
    // D1: SAVE RECIPE - Valid recipe persistence
    // =========================================================================
    @Test
    @DisplayName("D1-TRUE: saveRecipe persists recipe successfully")
    void testSaveRecipeSuccessfully() {
        // Precondition: Database is empty for test UUID
        // Input: Valid Recipe object
        // Expected: Recipe saved to DB, retrievable by ID
        Recipe recipe = TestRecipeBuilder.aRecipe()
            .withId(testUuid1)
            .withName("Tomato Pasta")
            .withDescription("Classic Italian pasta with tomato sauce")
            .build();

        recipeRepo.saveRecipe(recipe);

        Optional<Recipe> retrieved = recipeRepo.getRecipeById(testUuid1);
        assertTrue(retrieved.isPresent(), "Recipe should be found after save");
        assertEquals("Tomato Pasta", retrieved.get().getName());
        assertEquals("Classic Italian pasta with tomato sauce", retrieved.get().getDescription());
    }

    // =========================================================================
    // D2: GET RECIPE BY ID - Cache hit scenario (found)
    // =========================================================================
    @Test
    @DisplayName("D2-TRUE: getRecipeById returns recipe when present")
    void testGetRecipeByIdWhenPresent() {
        // Precondition: Recipe exists in DB
        // Input: Valid UUID of existing recipe
        // Expected: Recipe returned
        Recipe recipe = TestRecipeBuilder.aRecipe()
            .withId(testUuid2)
            .withName("Pasta Carbonara")
            .build();

        recipeRepo.saveRecipe(recipe);

        Optional<Recipe> result = recipeRepo.getRecipeById(testUuid2);

        assertTrue(result.isPresent());
        assertEquals(testUuid2, result.get().getId());
        assertEquals("Pasta Carbonara", result.get().getName());
    }

    // =========================================================================
    // D3: GET RECIPE BY ID - Cache miss scenario (not found)
    // =========================================================================
    @Test
    @DisplayName("D3-FALSE: getRecipeById returns empty for non-existent UUID")
    void testGetRecipeByIdWhenNotPresent() {
        // Precondition: UUID doesn't exist in DB
        // Input: Non-existent UUID
        // Expected: Optional.empty() returned
        String nonExistentUuid = UUID.randomUUID().toString();

        Optional<Recipe> result = recipeRepo.getRecipeById(nonExistentUuid);

        assertFalse(result.isPresent(), "Should return empty Optional for non-existent recipe");
    }

    // =========================================================================
    // BOUNDARY VALUE ANALYSIS: saveRecipe with null/blank IDs
    // =========================================================================
    @Test
    @DisplayName("BVA: saveRecipe with null ID throws exception")
    void testSaveRecipeWithNullId() {
        // Boundary: null ID
        Recipe recipe = TestRecipeBuilder.aRecipe().withId(null).build();

        assertThrows(IllegalArgumentException.class,
            () -> recipeRepo.saveRecipe(recipe),
            "Should throw exception for null recipe ID");
    }

    @Test
    @DisplayName("BVA: saveRecipe with blank ID throws exception")
    void testSaveRecipeWithBlankId() {
        // Boundary: blank/whitespace ID
        Recipe recipe = TestRecipeBuilder.aRecipe().withId("   ").build();

        assertThrows(IllegalArgumentException.class,
            () -> recipeRepo.saveRecipe(recipe),
            "Should throw exception for blank recipe ID");
    }

    // =========================================================================
    // D4: GET RECIPES BY IDS - Multiple recipe retrieval with order preservation
    // =========================================================================
    @Test
    @DisplayName("D4: getRecipesByIds retrieves multiple recipes in order")
    void testGetRecipesByIdsMultiple() {
        // Precondition: Multiple recipes exist in DB
        // Input: List of UUIDs [uuid1, uuid2, uuid3]
        // Expected: All recipes returned in same order
        Recipe recipe1 = TestRecipeBuilder.aRecipe().withId(testUuid1).withName("Recipe 1").build();
        Recipe recipe2 = TestRecipeBuilder.aRecipe().withId(testUuid2).withName("Recipe 2").build();
        Recipe recipe3 = TestRecipeBuilder.aRecipe().withId(testUuid3).withName("Recipe 3").build();

        recipeRepo.saveRecipe(recipe1);
        recipeRepo.saveRecipe(recipe2);
        recipeRepo.saveRecipe(recipe3);

        List<Recipe> results = recipeRepo.getRecipesByIds(List.of(testUuid1, testUuid2, testUuid3));

        assertEquals(3, results.size(), "Should retrieve all 3 recipes");
        assertEquals(testUuid1, results.get(0).getId(), "First recipe ID should match");
        assertEquals(testUuid2, results.get(1).getId(), "Second recipe ID should match");
        assertEquals(testUuid3, results.get(2).getId(), "Third recipe ID should match");
    }

    // =========================================================================
    // EQUIVALENCE PARTITIONING: getRecipesByIds with empty/partial results
    // =========================================================================
    @Test
    @DisplayName("EP: getRecipesByIds with empty list returns empty")
    void testGetRecipesByIdsWithEmptyList() {
        // Partition: empty input list
        List<Recipe> results = recipeRepo.getRecipesByIds(List.of());

        assertTrue(results.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    @DisplayName("EP: getRecipesByIds with null list returns empty")
    void testGetRecipesByIdsWithNullList() {
        // Partition: null input
        List<Recipe> results = recipeRepo.getRecipesByIds(null);

        assertTrue(results.isEmpty(), "Should return empty list for null input");
    }

    @Test
    @DisplayName("EP: getRecipesByIds with partial non-existent UUIDs")
    void testGetRecipesByIdsWithPartialResults() {
        // Partition: some UUIDs exist, some don't
        Recipe recipe1 = TestRecipeBuilder.aRecipe().withId(testUuid1).build();
        recipeRepo.saveRecipe(recipe1);

        String nonExistentUuid = UUID.randomUUID().toString();
        List<Recipe> results = recipeRepo.getRecipesByIds(List.of(testUuid1, nonExistentUuid));

        assertEquals(1, results.size(), "Should only return recipes that exist");
        assertEquals(testUuid1, results.get(0).getId());
    }

    // =========================================================================
    // BOUNDARY VALUE ANALYSIS: Duplicate UUIDs handling
    // =========================================================================
    @Test
    @DisplayName("BVA: getRecipesByIds with duplicate UUIDs deduplicates")
    void testGetRecipesByIdsWithDuplicates() {
        // Boundary: duplicate UUIDs in input list
        Recipe recipe = TestRecipeBuilder.aRecipe().withId(testUuid1).build();
        recipeRepo.saveRecipe(recipe);

        List<Recipe> results = recipeRepo.getRecipesByIds(
            List.of(testUuid1, testUuid1, testUuid1)
        );

        assertEquals(1, results.size(), "Duplicates should be removed");
        assertEquals(testUuid1, results.get(0).getId());
    }

    // =========================================================================
    // STATEMENT COVERAGE: Recipe update (delete then insert)
    // =========================================================================
    @Test
    @DisplayName("Statement Coverage: Update recipe (overwrite existing)")
    void testUpdateRecipe() {
        // Precondition: Recipe exists in DB
        // Input: New Recipe object with same ID but different data
        // Expected: Old record replaced with new data
        Recipe original = TestRecipeBuilder.aRecipe()
            .withId(testUuid1)
            .withName("Original Name")
            .build();

        recipeRepo.saveRecipe(original);

        Recipe updated = TestRecipeBuilder.aRecipe()
            .withId(testUuid1)
            .withName("Updated Name")
            .build();

        recipeRepo.saveRecipe(updated);

        Optional<Recipe> result = recipeRepo.getRecipeById(testUuid1);

        assertTrue(result.isPresent());
        assertEquals("Updated Name", result.get().getName());
    }

    // =========================================================================
    // DECISION COVERAGE: Null recipe object
    // =========================================================================
    @Test
    @DisplayName("Decision Coverage: saveRecipe with null object")
    void testSaveNullRecipe() {
        // Precondition: null recipe passed to save
        // Expected: Exception thrown (NullPointerException or IllegalArgumentException)
        assertThrows(Exception.class, () -> recipeRepo.saveRecipe(null));
    }
}

