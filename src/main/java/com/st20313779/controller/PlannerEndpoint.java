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
import java.time.format.DateTimeParseException;

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
        try {
            return recipeService.getRecipes(nameOrDesc, category, cuisine, difficulty, dietary, ingredients, page, perPage);
        } catch (RuntimeException ex) {
            throw mapServiceFailure(ex);
        }
    }


    @POST
    @Path("/save/day")
    public DayPlan saveDayPlan(@RequestBody final DayMealsDto dto) {
        try {
            return plannerService.saveDayPlan(dto);
        } catch (RuntimeException ex) {
            throw mapServiceFailure(ex);
        }
    }

    @GET
    @Path("/view/day/{date}")
    public DayPlan getDayPlan(@PathParam("date") final String date) {
        final LocalDate parsedDate = parseDate(date);
        try {
            return plannerService.getDayPlan(parsedDate);
        } catch (RuntimeException ex) {
            throw mapServiceFailure(ex);
        }
    }

    private LocalDate parseDate(final String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date format. Expected ISO-8601 yyyy-MM-dd", ex);
        }
    }

    private RuntimeException mapServiceFailure(final RuntimeException ex) {
        if (ex instanceof IllegalArgumentException) {
            return new BadRequestException(ex.getMessage());
        }
        if (ex instanceof WebApplicationException || ex instanceof ServiceLayerException) {
            return ex;
        }
        return new ServiceLayerException("Service layer failure", ex);
    }

}
