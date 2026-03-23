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

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    RecipeRepo recipeRepo;

    @Mock
    RecipeClient recipeClient;

    RecipeService recipeService;

    @BeforeEach
    void setUp() throws Exception {
        recipeService = new RecipeService(recipeRepo);
        Field clientField = RecipeService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(recipeService, recipeClient);
    }

    @Test
    void shouldDelegateGetRecipesToClientWithApiKey() {
        // Arrange
        RecipeListResponse expected = new RecipeListResponse();
        when(recipeClient.getRecipes(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);

        // Act
        RecipeListResponse actual = recipeService.getRecipes("pasta", "dinner", "italian", "easy", "vegetarian", "tomato", 1, 10);

        // Assert
        assertSame(expected, actual);
        verify(recipeClient).getRecipes(
                RecipeService.API_KEY,
                "pasta",
                "dinner",
                "italian",
                "easy",
                "vegetarian",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "tomato",
                1,
                10
        );
    }

    @Test
    void shouldReturnCachedRecipeWhenPresentInRepository() {
        // Arrange
        String uuid = "7be788d4-a8d8-4ab8-bf20-878ec1ef7779";
        Recipe cached = recipe(uuid);
        when(recipeRepo.getRecipeById(uuid)).thenReturn(Optional.of(cached));

        // Act
        Recipe result = recipeService.getRecipeById(uuid);

        // Assert
        assertSame(cached, result);
        verify(recipeRepo).getRecipeById(uuid);
        verify(recipeClient, never()).getRecipeById(any(), any(UUID.class));
        verify(recipeRepo, never()).saveRecipe(any());
    }

    @Test
    void shouldFetchAndPersistRecipeWhenNotInRepository() {
        // Arrange
        String uuid = "6e7db5a6-5f15-4e75-bcb0-126b92147f87";
        Recipe fetched = recipe(uuid);
        when(recipeRepo.getRecipeById(uuid)).thenReturn(Optional.empty());
        when(recipeClient.getRecipeById(RecipeService.API_KEY, UUID.fromString(uuid)))
                .thenReturn(new RecipeResponse(fetched, null));

        // Act
        Recipe result = recipeService.getRecipeById(uuid);

        // Assert
        assertEquals(uuid, result.getId());
        verify(recipeClient).getRecipeById(RecipeService.API_KEY, UUID.fromString(uuid));
        verify(recipeRepo).saveRecipe(fetched);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUuidIsMalformed() {
        // Arrange

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recipeService.getRecipeById("not-a-uuid"));

        // Assert
        assertEquals("Invalid UUID string: not-a-uuid", ex.getMessage());
        verify(recipeRepo).getRecipeById("not-a-uuid");
        verify(recipeClient, never()).getRecipeById(any(), any(UUID.class));
    }

    private Recipe recipe(final String id) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        return recipe;
    }
}

