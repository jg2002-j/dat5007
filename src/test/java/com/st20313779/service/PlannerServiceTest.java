package com.st20313779.service;

import com.st20313779.model.DayPlan;
import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.PlannerRepo;
import com.st20313779.repository.RecipeRepo;
import com.st20313779.repository.dto.DayMealsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @InjectMocks
    PlannerService plannerService;

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldThrowIllegalArgumentException_whenDtoIsNull() {
        // Arrange

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> plannerService.saveDayPlan(null));

        // Assert
        assertEquals("date is required", exception.getMessage());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldThrowIllegalArgumentException_whenDateIsNull() {
        // Arrange
        DayMealsDto dto = new DayMealsDto(null, new HashMap<>());

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> plannerService.saveDayPlan(dto));

        // Assert
        assertEquals("date is required", exception.getMessage());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldReturnEmptyPlan_whenMealMapIsNull() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), null);

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertEquals(date, result.getDate());
        assertNotNull(result.getMeals());
        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldReturnEmptyPlan_whenMealMapIsEmpty() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), new HashMap<>());

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertEquals(date, result.getDate());
        assertNotNull(result.getMeals());
        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldSkipNullSlotEntry() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(null, List.of("uuid-1"));
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), mealUuids);

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertNotNull(result.getMeals());
        assertTrue(result.getMeals().isEmpty());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldTreatNullUuidListAsEmpty_forValidSlot() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, null);
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), mealUuids);

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertTrue(result.getMeals().containsKey(MealSlot.BREAKFAST));
        assertTrue(result.getMeals().get(MealSlot.BREAKFAST).isEmpty());
        verifyNoInteractions(plannerRepo, recipeService, recipeRepo);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldSkipNullAndBlankUuids_andTrimValidUuid() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        String uuid = "uuid-1";
        Recipe recipe = recipe(uuid);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.DINNER, Arrays.asList(null, "   ", "", " " + uuid + " "));
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), mealUuids);
        when(recipeService.getRecipeById(uuid)).thenReturn(recipe);

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertTrue(result.getMeals().containsKey(MealSlot.DINNER));
        assertEquals(1, result.getMeals().get(MealSlot.DINNER).size());
        assertEquals(uuid, result.getMeals().get(MealSlot.DINNER).get(0).getId());
        verify(recipeService).getRecipeById(uuid);
        verify(recipeRepo).saveRecipe(recipe);
        verify(plannerRepo).saveSlotMealUuid(Date.valueOf(date), MealSlot.DINNER, uuid);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldPersistAllResolvedRecipes_acrossMultipleSlots() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        String breakfastUuid = "uuid-breakfast";
        String lunchUuid1 = "uuid-lunch-1";
        String lunchUuid2 = "uuid-lunch-2";
        Recipe breakfastRecipe = recipe(breakfastUuid);
        Recipe lunchRecipe1 = recipe(lunchUuid1);
        Recipe lunchRecipe2 = recipe(lunchUuid2);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of(breakfastUuid));
        mealUuids.put(MealSlot.LUNCH, List.of(lunchUuid1, lunchUuid2));
        DayMealsDto dto = new DayMealsDto(Date.valueOf(date), mealUuids);
        when(recipeService.getRecipeById(breakfastUuid)).thenReturn(breakfastRecipe);
        when(recipeService.getRecipeById(lunchUuid1)).thenReturn(lunchRecipe1);
        when(recipeService.getRecipeById(lunchUuid2)).thenReturn(lunchRecipe2);

        // Act
        DayPlan result = plannerService.saveDayPlan(dto);

        // Assert
        assertEquals(1, result.getMeals().get(MealSlot.BREAKFAST).size());
        assertEquals(2, result.getMeals().get(MealSlot.LUNCH).size());
        verify(recipeService).getRecipeById(breakfastUuid);
        verify(recipeService).getRecipeById(lunchUuid1);
        verify(recipeService).getRecipeById(lunchUuid2);
        verify(recipeRepo).saveRecipe(breakfastRecipe);
        verify(recipeRepo).saveRecipe(lunchRecipe1);
        verify(recipeRepo).saveRecipe(lunchRecipe2);
        verify(plannerRepo).saveSlotMealUuid(Date.valueOf(date), MealSlot.BREAKFAST, breakfastUuid);
        verify(plannerRepo).saveSlotMealUuid(Date.valueOf(date), MealSlot.LUNCH, lunchUuid1);
        verify(plannerRepo).saveSlotMealUuid(Date.valueOf(date), MealSlot.LUNCH, lunchUuid2);
    }

    @Test
    void basisPath_getDayPlan_shouldReturnEmptyMeals_whenRepoReturnsNoSlots() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        when(plannerRepo.getDayPlan(Date.valueOf(date))).thenReturn(new DayMealsDto(Date.valueOf(date), new HashMap<>()));

        // Act
        DayPlan result = plannerService.getDayPlan(date);

        // Assert
        assertEquals(date, result.getDate());
        assertTrue(result.getMeals().isEmpty());
        verify(plannerRepo).getDayPlan(Date.valueOf(date));
        verifyNoInteractions(recipeRepo);
    }

    @Test
    void basisPath_getDayPlan_shouldLoadRecipesForEachSlot_whenRepoReturnsMealUuids() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of("id-1", "id-2"));
        mealUuids.put(MealSlot.DINNER, List.of("id-3"));
        when(plannerRepo.getDayPlan(Date.valueOf(date))).thenReturn(new DayMealsDto(Date.valueOf(date), mealUuids));
        when(recipeRepo.getRecipesByIds(List.of("id-1", "id-2"))).thenReturn(List.of(recipe("id-1"), recipe("id-2")));
        when(recipeRepo.getRecipesByIds(List.of("id-3"))).thenReturn(List.of(recipe("id-3")));

        // Act
        DayPlan result = plannerService.getDayPlan(date);

        // Assert
        assertEquals(2, result.getMeals().size());
        assertEquals(2, result.getMeals().get(MealSlot.BREAKFAST).size());
        assertEquals(1, result.getMeals().get(MealSlot.DINNER).size());
        verify(plannerRepo).getDayPlan(Date.valueOf(date));
        verify(recipeRepo).getRecipesByIds(List.of("id-1", "id-2"));
        verify(recipeRepo).getRecipesByIds(List.of("id-3"));
    }

    @Test
    void basisPath_getDayPlan_shouldPassNullUuidListToRecipeRepo_whenRepoContainsNullList() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.LUNCH, null);
        when(plannerRepo.getDayPlan(Date.valueOf(date))).thenReturn(new DayMealsDto(Date.valueOf(date), mealUuids));
        when(recipeRepo.getRecipesByIds(isNull())).thenReturn(List.of());

        // Act
        DayPlan result = plannerService.getDayPlan(date);

        // Assert
        assertTrue(result.getMeals().containsKey(MealSlot.LUNCH));
        assertTrue(result.getMeals().get(MealSlot.LUNCH).isEmpty());
        verify(plannerRepo).getDayPlan(Date.valueOf(date));
        verify(recipeRepo).getRecipesByIds(null);
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldNotCallRecipeService_whenOnlyNullBlankOrWhitespaceUuidsExist() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.OTHER, Arrays.asList(null, "", "   "));

        // Act
        DayPlan result = plannerService.saveDayPlan(new DayMealsDto(Date.valueOf(date), mealUuids));

        // Assert
        assertTrue(result.getMeals().containsKey(MealSlot.OTHER));
        assertTrue(result.getMeals().get(MealSlot.OTHER).isEmpty());
        verify(recipeService, never()).getRecipeById(org.mockito.ArgumentMatchers.anyString());
        verify(recipeRepo, never()).saveRecipe(org.mockito.ArgumentMatchers.any());
        verify(plannerRepo, never()).saveSlotMealUuid(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void basisPath_mapDayMealsToDayPlan_shouldInvokePersistenceLoopOncePerResolvedRecipe() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 16);
        Map<MealSlot, List<String>> mealUuids = new HashMap<>();
        mealUuids.put(MealSlot.BREAKFAST, List.of("r1", "r2"));
        when(recipeService.getRecipeById("r1")).thenReturn(recipe("r1"));
        when(recipeService.getRecipeById("r2")).thenReturn(recipe("r2"));

        // Act
        plannerService.saveDayPlan(new DayMealsDto(Date.valueOf(date), mealUuids));

        // Assert
        verify(recipeRepo, times(2)).saveRecipe(org.mockito.ArgumentMatchers.any());
        verify(plannerRepo, times(2)).saveSlotMealUuid(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private Recipe recipe(final String id) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        return recipe;
    }
}


