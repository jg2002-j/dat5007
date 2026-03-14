package com.st20313779.service;

import com.st20313779.model.recipe.Recipe;
import com.st20313779.model.recipe.RecipeResponse;
import com.st20313779.repository.RecipeRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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

    public Recipe getRecipeById(final String uuid) {



        final RecipeResponse recipeRes = client.getRecipeById(API_KEY, UUID.fromString(uuid));
        final Recipe recipe = recipeRes.getData();
        repo.saveRecipe(recipe);
        return recipe;
    }

}
