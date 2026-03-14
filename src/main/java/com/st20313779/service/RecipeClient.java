package com.st20313779.service;

import com.st20313779.model.recipe.RecipeListResponse;
import com.st20313779.model.recipe.RecipeResponse;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "recipe-api")
public interface RecipeClient {

    @GET
    @Path("/recipes")
    RecipeListResponse getRecipes(
            @HeaderParam("X-API-Key") String apiKey,
            @QueryParam("q") String nameOrDesc,
            @QueryParam("category") String category,
            @QueryParam("cuisine") String cuisine,
            @QueryParam("difficulty") String difficulty,
            @QueryParam("dietary") String dietary,
            @QueryParam("min_calories") Double minCalories,
            @QueryParam("max_calories") Double maxCalories,
            @QueryParam("min_protein") Double minProtein,
            @QueryParam("max_protein") Double maxProtein,
            @QueryParam("min_carbs") Double minCarbs,
            @QueryParam("max_carbs") Double maxCarbs,
            @QueryParam("min_fat") Double minFat,
            @QueryParam("max_fat") Double maxFat,
            @QueryParam("ingredients") String ingredients,
            @QueryParam("page") Integer page,
            @QueryParam("per_page") Integer perPage
    );

    @GET
    @Path("/recipes/{id}")
    RecipeResponse getRecipeById(
            @HeaderParam("X-API-Key") String apiKey,
            @PathParam("id") UUID id
    );
}
