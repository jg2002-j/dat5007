package com.st20313779.controller;

import com.st20313779.model.DayPlan;
import com.st20313779.model.recipe.RecipeListResponse;
import com.st20313779.repository.dto.DayMealsDto;
import com.st20313779.service.PlannerService;
import com.st20313779.service.RecipeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.time.LocalDate;

@Path("planner")
@ApplicationScoped
public class PlannerEndpoint {

    private final PlannerService plannerService;
    private final RecipeService recipeService;

    @Inject
    public PlannerEndpoint(final PlannerService plannerService, final RecipeService recipeService) {
        this.plannerService = plannerService;
        this.recipeService = recipeService;
    }

    @GET
    @Path("/recipes")
    public RecipeListResponse getRecipes(
            @QueryParam("q") String nameOrDesc,
            @QueryParam("category") String category,
            @QueryParam("cuisine") String cuisine,
            @QueryParam("difficulty") String difficulty,
            @QueryParam("dietary") String dietary,
            @QueryParam("ingredients") String ingredients,
            @QueryParam("page") Integer page,
            @QueryParam("per_page") Integer perPage
    ) {
        return recipeService.getRecipes(nameOrDesc, category, cuisine, difficulty, dietary, ingredients, page, perPage);
    }


    @POST
    @Path("/save/day")
    public DayPlan saveDayPlan(@RequestBody final DayMealsDto dto) {
        return plannerService.saveDayPlan(dto);
    }

    @GET
    @Path("/view/day/{date}")
    public DayPlan getDayPlan(@PathParam("date") final LocalDate date) {
        return plannerService.getDayPlan(date);
    }

}
