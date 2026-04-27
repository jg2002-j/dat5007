package com.st20313779.service;

import com.st20313779.fixtures.TestRecipeBuilder;
import com.st20313779.model.DayPlan;
import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.PlannerRepo;
import com.st20313779.repository.RecipeRepo;
import com.st20313779.repository.dto.DayMealsDto;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit Test for PlannerService class covering all independent paths via Basis Path Testing.
 *
 * =========================================================================
 * PHASE 2: BASIS PATH TESTING & CONTROL FLOW GRAPH (CFG)
 * =========================================================================
 *
 * METHOD: saveDayPlan(DayMealsDto dto)
 *
 * CONTROL FLOW GRAPH:
 * ```
 *   ENTRY
 *     ├─ Decision D1: if (dto == null || dto.getDate() == null)
 *     │   ├─ TRUE → THROW IllegalArgumentException → EXIT
 *     │   └─ FALSE → Continue to D2
 *     ├─ Decision D2: if (dto.getMealUuids() != null)
 *     │   ├─ TRUE → Loop through slotEntry (D3)
 *     │   │   ├─ Decision D3: if (slot == null)
 *     │   │   │   ├─ TRUE → continue
 *     │   │   │   └─ FALSE → Process slot
 *     │   │   └─ Inner Loop D4: for each uuid
 *     │   │       ├─ Decision D4: if (trimmedUuid.isBlank())
 *     │   │       │   ├─ TRUE → continue
 *     │   │       │   └─ FALSE → Add recipe
 *     │   └─ FALSE → Skip to EXIT
 *     └─ RETURN DayPlan → EXIT
 * ```
 *
 * CYCLOMATIC COMPLEXITY CALCULATION:
 * V(G) = Edges - Nodes + 2 = 4 decision points + 1 = 5
 * Required independent paths: 5
 *
 * INDEPENDENT PATHS IDENTIFIED:
 * 1. Path 1: dto = null → IllegalArgumentException (D1=TRUE)
 * 2. Path 2: dto.date = null → IllegalArgumentException (D1=TRUE)
 * 3. Path 3: dto valid, mealUuids = null → Empty map returned (D2=FALSE)
 * 4. Path 4: dto valid, mealUuids valid, all uuids blank → Skip all (D3=FALSE, D4=TRUE)
 * 5. Path 5: dto valid, mealUuids valid, valid uuids → Success path (D2=TRUE, D3=FALSE, D4=FALSE)
 *
 * =========================================================================
 * RTM MAPPING:
 * TR-002: Save valid meal plan
 * TR-003: Save meal plan with null date (error case)
 * TR-004: Save meal plan with empty meal slots
 * TR-005: Save meal plan with blank UUIDs
 * TR-006: Save meal plan with duplicate RecipeService calls
 * =========================================================================
 */
@QuarkusTest
@DisplayName("PlannerService Unit Tests - Basis Path Analysis")
public class PlannerServiceTest {

    private PlannerService plannerService;

    @InjectMock
    RecipeService recipeService;

    @InjectMock
    PlannerRepo plannerRepo;

    @InjectMock
    RecipeRepo recipeRepo;

    @BeforeEach
    void setUp() {
        plannerService = new PlannerService(plannerRepo, recipeService, recipeRepo);
    }

    // =========================================================================
    // INDEPENDENT PATH 1: Null DTO (D1=TRUE)
    // =========================================================================
    @Test
    @DisplayName("Path 1: saveDayPlan with null DTO throws IllegalArgumentException")
    void testSaveDayPlanWithNullDto() {
        // Precondition: dto is null
        // Input: null
        // Expected: IllegalArgumentException with message "date is required"
        assertThrows(IllegalArgumentException.class,
            () -> plannerService.saveDayPlan(null),
            "Expected IllegalArgumentException for null DTO");
    }

    // =========================================================================
    // INDEPENDENT PATH 2: Null Date in DTO (D1=TRUE)
    // =========================================================================
    @Test
    @DisplayName("Path 2: saveDayPlan with null date throws IllegalArgumentException")
    void testSaveDayPlanWithNullDate() {
        // Precondition: DTO with null date
        // Input: DayMealsDto { date: null, mealUuids: {} }
        // Expected: IllegalArgumentException
        DayMealsDto dto = new DayMealsDto().setDate(null).setMealUuids(new HashMap<>());
        assertThrows(IllegalArgumentException.class,
            () -> plannerService.saveDayPlan(dto),
            "Expected IllegalArgumentException for null date");
    }

    // =========================================================================
    // INDEPENDENT PATH 3: Null mealUuids (D2=FALSE)
    // =========================================================================
    @Test
    @DisplayName("Path 3: saveDayPlan with null mealUuids returns empty DayPlan")
    void testSaveDayPlanWithNullMealUuids() {
        // Precondition: DTO with valid date but null mealUuids
        // Input: DayMealsDto { date: 2026-04-27, mealUuids: null }
        // Expected: DayPlan with empty meals map
        Date date = Date.valueOf("2026-04-27");
        DayMealsDto dto = new DayMealsDto().setDate(date).setMealUuids(null);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertNotNull(result, "DayPlan should not be null");
        assertEquals(date.toLocalDate(), result.getDate(), "Date should match");
        assertTrue(result.getMeals().isEmpty(), "Meals map should be empty");
        verify(plannerRepo, never()).saveSlotMealUuid(any(), any(), any());
    }

    // =========================================================================
    // INDEPENDENT PATH 4: All UUIDs blank (D4=TRUE for all)
    // =========================================================================
    @Test
    @DisplayName("Path 4: saveDayPlan with all blank UUIDs returns empty DayPlan")
    void testSaveDayPlanWithAllBlankUuids() {
        // Precondition: Valid DTO with meal slots but all UUIDs are blank/whitespace
        // Input: mealUuids { BREAKFAST: ["   "], LUNCH: [""] }
        // Expected: Empty meals map, no recipeService calls
        Date date = Date.valueOf("2026-04-27");
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of("   ", "\t", ""));
        DayMealsDto dto = new DayMealsDto().setDate(date).setMealUuids(mealUuids);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertNotNull(result, "DayPlan should not be null");
        assertTrue(result.getMeals().containsKey(MealSlot.BREAKFAST), "Slot should be present even when UUIDs are blank");
        assertTrue(result.getMeals().get(MealSlot.BREAKFAST).isEmpty(), "Recipes list should be empty when all UUIDs are blank");
        verify(recipeService, never()).getRecipeById(anyString());
    }

    // =========================================================================
    // INDEPENDENT PATH 5: Valid flow with recipes (D2=TRUE, D3=FALSE, D4=FALSE)
    // =========================================================================
    @Test
    @DisplayName("Path 5: saveDayPlan with valid UUIDs persists successfully")
    void testSaveDayPlanWithValidUuids() {
        // Precondition: Valid DTO with proper UUIDs
        // Input: mealUuids { BREAKFAST: ["uuid-1"], LUNCH: ["uuid-2", "uuid-3"] }
        // Expected: DayPlan with populated meals, repos called appropriately
        Date date = Date.valueOf("2026-04-27");
        String uuid1 = "550e8400-e29b-41d4-a716-446655440001";
        String uuid2 = "550e8400-e29b-41d4-a716-446655440002";
        String uuid3 = "550e8400-e29b-41d4-a716-446655440003";

        Recipe recipe1 = TestRecipeBuilder.aRecipe().withId(uuid1).withName("Pasta").build();
        Recipe recipe2 = TestRecipeBuilder.aRecipe().withId(uuid2).withName("Salad").build();
        Recipe recipe3 = TestRecipeBuilder.aRecipe().withId(uuid3).withName("Dessert").build();

        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of(uuid1));
        mealUuids.put(MealSlot.LUNCH, List.of(uuid2, uuid3));
        DayMealsDto dto = new DayMealsDto().setDate(date).setMealUuids(mealUuids);

        when(recipeService.getRecipeById(uuid1)).thenReturn(recipe1);
        when(recipeService.getRecipeById(uuid2)).thenReturn(recipe2);
        when(recipeService.getRecipeById(uuid3)).thenReturn(recipe3);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertNotNull(result, "DayPlan should not be null");
        assertEquals(date.toLocalDate(), result.getDate());
        assertEquals(2, result.getMeals().size(), "Should have 2 meal slots");
        assertEquals(1, result.getMeals().get(MealSlot.BREAKFAST).size());
        assertEquals(2, result.getMeals().get(MealSlot.LUNCH).size());

        verify(recipeService, times(3)).getRecipeById(anyString());
        verify(plannerRepo, times(3)).saveSlotMealUuid(eq(date), any(), anyString());
    }

    // =========================================================================
    // EQUIVALENCE PARTITIONING TESTS: getDayPlan
    // =========================================================================
    @Test
    @DisplayName("EP: getDayPlan with existing date returns populated plan")
    void testGetDayPlanWithExistingDate() {
        // Partition: Date with existing data
        LocalDate date = LocalDate.of(2026, 4, 27);
        String uuid1 = "550e8400-e29b-41d4-a716-446655440001";

        DayMealsDto storedPlan = new DayMealsDto()
            .setDate(Date.valueOf(date))
            .setMealUuids(Map.of(MealSlot.BREAKFAST, List.of(uuid1)));

        Recipe recipe = TestRecipeBuilder.aRecipe().withId(uuid1).build();

        when(plannerRepo.getDayPlan(any())).thenReturn(storedPlan);
        when(recipeRepo.getRecipesByIds(anyList())).thenReturn(List.of(recipe));

        DayPlan result = plannerService.getDayPlan(date);

        assertEquals(date, result.getDate());
        assertEquals(1, result.getMeals().get(MealSlot.BREAKFAST).size());
        verify(plannerRepo).getDayPlan(Date.valueOf(date));
    }

    @Test
    @DisplayName("EP: getDayPlan with non-existent date returns empty plan")
    void testGetDayPlanWithNonExistentDate() {
        // Partition: Date with no existing data
        LocalDate date = LocalDate.of(2025, 1, 1);

        DayMealsDto emptyPlan = new DayMealsDto()
            .setDate(Date.valueOf(date))
            .setMealUuids(new HashMap<>());

        when(plannerRepo.getDayPlan(any())).thenReturn(emptyPlan);

        DayPlan result = plannerService.getDayPlan(date);

        assertEquals(date, result.getDate());
        assertTrue(result.getMeals().isEmpty());
    }

    // =========================================================================
    // BOUNDARY VALUE ANALYSIS TESTS: Date boundaries
    // =========================================================================
    @ParameterizedTest
    @ValueSource(strings = {"1900-01-01", "2026-04-27", "2099-12-31"})
    @DisplayName("BVA: getDayPlan with boundary dates (min, current, max)")
    void testGetDayPlanWithBoundaryDates(String dateString) {
        // Boundary test: Min date (1900), current date, max date (2099)
        LocalDate date = LocalDate.parse(dateString);
        DayMealsDto emptyPlan = new DayMealsDto()
            .setDate(Date.valueOf(date))
            .setMealUuids(new HashMap<>());

        when(plannerRepo.getDayPlan(any())).thenReturn(emptyPlan);

        DayPlan result = plannerService.getDayPlan(date);

        assertEquals(date, result.getDate());
        assertNotNull(result);
    }

    // =========================================================================
    // DECISION COVERAGE TESTS: Multiple meal slots per day
    // =========================================================================
    @Test
    @DisplayName("Decision Coverage: All meal slot types processed")
    void testSaveDayPlanWithAllMealSlots() {
        // Test all 4 meal slot types: BREAKFAST, LUNCH, DINNER, OTHER
        Date date = Date.valueOf("2026-04-27");
        String uuid1 = "550e8400-e29b-41d4-a716-446655440001";
        String uuid2 = "550e8400-e29b-41d4-a716-446655440002";
        String uuid3 = "550e8400-e29b-41d4-a716-446655440003";
        String uuid4 = "550e8400-e29b-41d4-a716-446655440004";

        Recipe recipe1 = TestRecipeBuilder.aRecipe().withId(uuid1).build();
        Recipe recipe2 = TestRecipeBuilder.aRecipe().withId(uuid2).build();
        Recipe recipe3 = TestRecipeBuilder.aRecipe().withId(uuid3).build();
        Recipe recipe4 = TestRecipeBuilder.aRecipe().withId(uuid4).build();

        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of(uuid1));
        mealUuids.put(MealSlot.LUNCH, List.of(uuid2));
        mealUuids.put(MealSlot.DINNER, List.of(uuid3));
        mealUuids.put(MealSlot.OTHER, List.of(uuid4));
        DayMealsDto dto = new DayMealsDto().setDate(date).setMealUuids(mealUuids);

        when(recipeService.getRecipeById(uuid1)).thenReturn(recipe1);
        when(recipeService.getRecipeById(uuid2)).thenReturn(recipe2);
        when(recipeService.getRecipeById(uuid3)).thenReturn(recipe3);
        when(recipeService.getRecipeById(uuid4)).thenReturn(recipe4);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertEquals(4, result.getMeals().size());
        assertTrue(result.getMeals().containsKey(MealSlot.BREAKFAST));
        assertTrue(result.getMeals().containsKey(MealSlot.LUNCH));
        assertTrue(result.getMeals().containsKey(MealSlot.DINNER));
        assertTrue(result.getMeals().containsKey(MealSlot.OTHER));
    }

    // =========================================================================
    // STATEMENT COVERAGE: Mixed valid and blank UUIDs
    // =========================================================================
    @Test
    @DisplayName("Statement Coverage: Mixed valid and blank UUIDs in same slot")
    void testSaveDayPlanWithMixedUuids() {
        // Some UUIDs valid, some blank in same list
        Date date = Date.valueOf("2026-04-27");
        String uuid1 = "550e8400-e29b-41d4-a716-446655440001";
        String uuid2 = "550e8400-e29b-41d4-a716-446655440002";

        Recipe recipe1 = TestRecipeBuilder.aRecipe().withId(uuid1).build();
        Recipe recipe2 = TestRecipeBuilder.aRecipe().withId(uuid2).build();

        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of(uuid1, "   ", uuid2, "", "\t"));
        DayMealsDto dto = new DayMealsDto().setDate(date).setMealUuids(mealUuids);

        when(recipeService.getRecipeById(uuid1)).thenReturn(recipe1);
        when(recipeService.getRecipeById(uuid2)).thenReturn(recipe2);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertEquals(1, result.getMeals().size());
        assertEquals(2, result.getMeals().get(MealSlot.BREAKFAST).size());
        verify(recipeService, times(2)).getRecipeById(anyString());
    }
}

