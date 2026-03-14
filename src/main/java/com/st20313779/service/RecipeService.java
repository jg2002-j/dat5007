package com.st20313779.service;

import com.st20313779.model.recipe.Recipe;
import com.st20313779.model.recipe.RecipeListResponse;
import com.st20313779.model.recipe.RecipeResponse;
import com.st20313779.repository.RecipeRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RecipeService {

    public static final String API_KEY = "rapi_160c31b9943b6b44659bf2263bb15b16fe2a264e36a15bda";

    @RestClient
    RecipeClient client;

    private final RecipeRepo repo;

    @Inject
    public RecipeService(final RecipeRepo repo) {
        this.repo = repo;
    }

    public RecipeListResponse getRecipes(
            final String nameOrDesc,
            final String category,
            final String cuisine,
            final String difficulty,
            final String dietary,
            final String ingredients,
            final Integer page,
            final Integer perPage
    ) {
        return client.getRecipes(
                API_KEY,
                nameOrDesc,
                category,
                cuisine,
                difficulty,
                dietary,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ingredients,
                page,
                perPage
        );
    }


    public Recipe getRecipeById(final String uuid) {
        Optional<Recipe> recipeOpt = repo.getRecipeById(uuid);
        if (recipeOpt.isPresent()) {
            return recipeOpt.get();

        } else {
            final RecipeResponse recipeRes = client.getRecipeById(API_KEY, UUID.fromString(uuid));
            Recipe recipe = recipeRes.getData();
            repo.saveRecipe(recipe);
            return recipe;
        }
    }


}
