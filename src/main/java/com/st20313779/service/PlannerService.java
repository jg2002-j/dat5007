package com.st20313779.service;

import com.st20313779.model.DayPlan;
import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.PlannerRepo;
import com.st20313779.repository.RecipeRepo;
import com.st20313779.repository.dto.DayMealsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class PlannerService {

    private final PlannerRepo plannerRepo;

    private final RecipeService recipeService;
    private final RecipeRepo recipeRepo;

    @Inject
    public PlannerService(final PlannerRepo plannerRepo, final RecipeService recipeService, final RecipeRepo recipeRepo) {
        this.plannerRepo = plannerRepo;
        this.recipeService = recipeService;
        this.recipeRepo = recipeRepo;
    }

    @Transactional
    public DayPlan saveDayPlan(final DayMealsDto dto) {
        final DayPlan dayPlan = mapDayMealsToDayPlan(dto);
        writeDayPlanToDb(dayPlan);
        return dayPlan;
    }

    private DayPlan mapDayMealsToDayPlan(final DayMealsDto dto) {
        if (dto == null || dto.getDate() == null) {
            throw new IllegalArgumentException("date is required");
        }

        final Map<MealSlot, List<Recipe>> recipeMap = new HashMap<>();
        if (dto.getMealUuids() != null) {
            for (final Map.Entry<MealSlot, List<String>> slotEntry : dto.getMealUuids().entrySet()) {
                final MealSlot slot = slotEntry.getKey();
                if (slot == null) {
                    continue;
                }

                final List<Recipe> recipes = new ArrayList<>();
                final List<String> uuids = slotEntry.getValue();
                for (final String uuid : uuids == null ? List.<String>of() : uuids.stream().filter(Objects::nonNull).toList()) {
                    final String trimmedUuid = uuid.trim();
                    if (trimmedUuid.isBlank()) {
                        continue;
                    }
                    recipes.add(recipeService.getRecipeById(trimmedUuid));
                }
                recipeMap.put(slot, recipes);
            }
        }

        return new DayPlan(dto.getDate().toLocalDate(), recipeMap);
    }

    private void writeDayPlanToDb(final DayPlan dayPlan) {
        final Date date = Date.valueOf(dayPlan.getDate());
        dayPlan.getMeals().forEach((slot, recipes) -> {
            for (final Recipe recipe : recipes) {
                recipeRepo.saveRecipe(recipe);
                plannerRepo.saveSlotMealUuid(date, slot, recipe.getId());
            }
        });
    }

    public DayPlan getDayPlan(final LocalDate date) {
        final DayMealsDto meals = plannerRepo.getDayPlan(Date.valueOf(date));
        final DayPlan dayPlan = new DayPlan(
                date,
                new HashMap<>()
        );

        for (final MealSlot slot : meals.getMealUuids().keySet()) {
            final List<Recipe> recipes = recipeRepo.getRecipesByIds(meals.getMealUuids().get(slot));
            dayPlan.getMeals().put(slot, recipes);
        }

        return dayPlan;
    }
}
