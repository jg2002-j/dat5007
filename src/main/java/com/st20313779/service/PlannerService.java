package com.st20313779.service;

import com.st20313779.model.DayPlan;
import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.PlannerRepo;
import com.st20313779.repository.RecipeRepo;
import com.st20313779.repository.dto.DayMealsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

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

    public DayPlan saveDayPlan(final DayMealsDto dto) {

    }

    public DayPlan getDayPlan(final LocalDate date) {
        final DayMealsDto meals = plannerRepo.getDayPlan(Date.valueOf(date));
        final DayPlan dayPlan = new DayPlan(
                date,
                new HashMap<>()
        );

        for (final MealSlot slot : meals.getMealUuids().keySet()) {
            final List<Recipe> recipes = recipeRepo.fetchRecipes(meals.getMealUuids().get(slot));
            dayPlan.getMeals().put(slot, recipes);
        }

        return dayPlan;
    }
}
