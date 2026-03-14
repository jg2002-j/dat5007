package com.st20313779.service;

import com.st20313779.model.DayPlan;
import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.PlannerRepo;
import com.st20313779.repository.RecipeRepo;
import com.st20313779.repository.dto.DayMealsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTest {

    @Mock
    PlannerRepo plannerRepo;

    @Mock
    RecipeService recipeService;

    @Mock
    RecipeRepo recipeRepo;

    PlannerService plannerService;

    @BeforeEach
    void setUp() {
        plannerService = new PlannerService(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void saveDayPlan_throwsWhenDtoIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> plannerService.saveDayPlan(null));

        assertEquals("date is required", ex.getMessage());
        verifyNoInteractions(plannerRepo, recipeRepo, recipeService);
    }

    @Test
    void saveDayPlan_throwsWhenDateIsNull() {
        DayMealsDto dto = new DayMealsDto().setDate(null).setMealUuids(Map.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> plannerService.saveDayPlan(dto));

        assertEquals("date is required", ex.getMessage());
        verifyNoInteractions(plannerRepo, recipeRepo, recipeService);
    }

    @Test
    void saveDayPlan_returnsEmptyMealsWhenMealMapIsNull() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(null);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertEquals(localDate, result.getDate());
        assertNotNull(result.getMeals());
        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(plannerRepo, recipeRepo, recipeService);
    }

    @Test
    void saveDayPlan_returnsEmptyMealsWhenMealMapIsEmpty() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(Map.of());

        DayPlan result = plannerService.saveDayPlan(dto);

        assertEquals(localDate, result.getDate());
        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(plannerRepo, recipeRepo, recipeService);
    }

    @Test
    void saveDayPlan_skipsNullMealSlot_ac2Logic() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        Map<MealSlot, List<String>> meals = new HashMap<>();
        meals.put(null, List.of("c6e56309-6cde-4f87-bbf8-a22f8a2fda4c"));
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(meals);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(recipeService, recipeRepo, plannerRepo);
    }

    @Test
    void saveDayPlan_handlesNullUuidListForValidSlot() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        Map<MealSlot, List<String>> meals = new HashMap<>();
        meals.put(MealSlot.LUNCH, null);
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(meals);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertTrue(result.getMeals().containsKey(MealSlot.LUNCH));
        assertTrue(result.getMeals().get(MealSlot.LUNCH).isEmpty());
        verifyNoInteractions(recipeService, recipeRepo, plannerRepo);
    }

    @Test
    void saveDayPlan_skipsBlankUuid_boundaryCase() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        Map<MealSlot, List<String>> meals = new HashMap<>();
        meals.put(MealSlot.DINNER, List.of("   ", ""));
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(meals);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertTrue(result.getMeals().containsKey(MealSlot.DINNER));
        assertTrue(result.getMeals().get(MealSlot.DINNER).isEmpty());
        verify(recipeService, never()).getRecipeById(any());
        verifyNoInteractions(recipeRepo, plannerRepo);
    }

    @Test
    void saveDayPlan_resolvesAndPersistsRecipesForValidUuid() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        String uuid = "c6e56309-6cde-4f87-bbf8-a22f8a2fda4c";

        Map<MealSlot, List<String>> meals = new HashMap<>();
        meals.put(MealSlot.BREAKFAST, List.of(uuid));
        DayMealsDto dto = new DayMealsDto().setDate(Date.valueOf(localDate)).setMealUuids(meals);

        Recipe recipe = new Recipe();
        recipe.setId(uuid);
        recipe.setName("Oats");

        when(recipeService.getRecipeById(uuid)).thenReturn(recipe);

        DayPlan result = plannerService.saveDayPlan(dto);

        assertEquals(1, result.getMeals().get(MealSlot.BREAKFAST).size());
        assertEquals(uuid, result.getMeals().get(MealSlot.BREAKFAST).get(0).getId());
        verify(recipeService).getRecipeById(uuid);
        verify(recipeRepo).saveRecipe(recipe);
        verify(plannerRepo).saveSlotMealUuid(eq(Date.valueOf(localDate)), eq(MealSlot.BREAKFAST), eq(uuid));
    }

    @Test
    void getDayPlan_loadsRecipeListsBySlot() {
        LocalDate localDate = LocalDate.of(2026, 3, 14);
        String uuid = "f9c13d22-91dd-4e6d-b72f-d286dd9f23fe";

        DayMealsDto storedMeals = new DayMealsDto()
                .setDate(Date.valueOf(localDate))
                .setMealUuids(Map.of(MealSlot.LUNCH, List.of(uuid)));

        Recipe recipe = new Recipe();
        recipe.setId(uuid);
        recipe.setName("Salad");

        when(plannerRepo.getDayPlan(Date.valueOf(localDate))).thenReturn(storedMeals);
        when(recipeRepo.getRecipesByIds(List.of(uuid))).thenReturn(List.of(recipe));

        DayPlan result = plannerService.getDayPlan(localDate);

        assertEquals(localDate, result.getDate());
        assertEquals(1, result.getMeals().get(MealSlot.LUNCH).size());
        assertEquals(uuid, result.getMeals().get(MealSlot.LUNCH).get(0).getId());
        verify(plannerRepo).getDayPlan(Date.valueOf(localDate));
        verify(recipeRepo).getRecipesByIds(List.of(uuid));
    }
}

