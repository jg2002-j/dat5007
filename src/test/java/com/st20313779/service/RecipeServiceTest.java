package com.st20313779.service;

import com.st20313779.model.recipe.Recipe;
import com.st20313779.model.recipe.RecipeListResponse;
import com.st20313779.model.recipe.RecipeResponse;
import com.st20313779.repository.RecipeRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    RecipeRepo repo;

    @Mock
    RecipeClient client;

    RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(repo);
        // Set the REST client mock directly for unit tests.
        recipeService.client = client;
    }

    @Test
    void getRecipes_forwardsParametersToClient() {
        RecipeListResponse response = new RecipeListResponse();

        when(client.getRecipes(
                eq(RecipeService.API_KEY),
                eq("salad"),
                eq("Main"),
                eq("Mediterranean"),
                eq("easy"),
                eq("vegetarian"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq("tomato"),
                eq(2),
                eq(10)
        )).thenReturn(response);

        RecipeListResponse result = recipeService.getRecipes(
                "salad",
                "Main",
                "Mediterranean",
                "easy",
                "vegetarian",
                "tomato",
                2,
                10
        );

        assertSame(response, result);
    }

    @Test
    void getRecipeById_returnsCachedRecipe_whenPresent() {
        String uuid = "35d6e02f-6c41-4a75-8f36-16280b6ca428";
        Recipe recipe = new Recipe();
        recipe.setId(uuid);

        when(repo.getRecipeById(uuid)).thenReturn(Optional.of(recipe));

        Recipe result = recipeService.getRecipeById(uuid);

        assertSame(recipe, result);
        verify(client, never()).getRecipeById(any(), any());
        verify(repo, never()).saveRecipe(any());
    }

    @Test
    void getRecipeById_fetchesFromApiAndCaches_whenMissing() {
        String uuid = "3dd59406-4a0a-4886-8f99-7d5771f44b91";
        Recipe recipe = new Recipe();
        recipe.setId(uuid);

        RecipeResponse apiResponse = new RecipeResponse();
        apiResponse.setData(recipe);

        when(repo.getRecipeById(uuid)).thenReturn(Optional.empty());
        when(client.getRecipeById(RecipeService.API_KEY, UUID.fromString(uuid))).thenReturn(apiResponse);

        Recipe result = recipeService.getRecipeById(uuid);

        assertSame(recipe, result);
        verify(client).getRecipeById(RecipeService.API_KEY, UUID.fromString(uuid));
        verify(repo).saveRecipe(recipe);
    }

    @Test
    void getRecipeById_throwsForInvalidUuid_whenNotCached() {
        when(repo.getRecipeById("not-a-uuid")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> recipeService.getRecipeById("not-a-uuid"));
    }
}

