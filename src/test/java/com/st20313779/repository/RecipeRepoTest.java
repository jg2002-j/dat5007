package com.st20313779.repository;

import com.st20313779.model.recipe.Dietary;
import com.st20313779.model.recipe.Equipment;
import com.st20313779.model.recipe.Ingredient;
import com.st20313779.model.recipe.IngredientGroup;
import com.st20313779.model.recipe.Instruction;
import com.st20313779.model.recipe.Nutrition;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.model.recipe.RecipeMeta;
import com.st20313779.model.recipe.Storage;
import com.st20313779.model.recipe.StructuredStep;
import com.st20313779.model.recipe.Troubleshooting;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RecipeRepoTest {

    @Inject
    RecipeRepo recipeRepo;

    @Inject
    DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("TRUNCATE TABLE public.meals, public.recipe RESTART IDENTITY CASCADE")) {
            ps.executeUpdate();
        }
    }

    @Test
    void shouldThrowWhenSavingRecipeWithoutId() {
        // Arrange
        Recipe recipe = new Recipe();

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> recipeRepo.saveRecipe(recipe));

        // Assert
        assertEquals("recipe.id is required", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUuidForGetByIdIsNullOrBlank() {
        // Arrange

        // Act
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () -> recipeRepo.getRecipeById(null));
        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () -> recipeRepo.getRecipeById("  "));

        // Assert
        assertEquals("uuid is required", exNull.getMessage());
        assertEquals("uuid is required", exBlank.getMessage());
    }

    @Test
    void shouldReturnEmptyForNullEmptyOrBlankIdsList() {
        // Arrange

        // Act
        List<Recipe> nullList = recipeRepo.getRecipesByIds(null);
        List<Recipe> emptyList = recipeRepo.getRecipesByIds(List.of());
        List<Recipe> blankOnly = recipeRepo.getRecipesByIds(List.of("  ", "\t"));

        // Assert
        assertTrue(nullList.isEmpty());
        assertTrue(emptyList.isEmpty());
        assertTrue(blankOnly.isEmpty());
    }

    @Test
    void shouldSaveAndHydrateRecipeWithNestedStructures() {
        // Arrange
        Recipe input = fullRecipe("recipe-full");

        // Act
        recipeRepo.saveRecipe(input);
        Optional<Recipe> result = recipeRepo.getRecipeById("recipe-full");

        // Assert
        assertTrue(result.isPresent());
        Recipe loaded = result.get();
        assertEquals("recipe-full", loaded.getId());
        assertEquals("Tomato Pasta", loaded.getName());
        assertEquals("Italian", loaded.getCuisine());

        assertNotNull(loaded.getMeta());
        assertEquals("35m", loaded.getMeta().getTotalTime());
        assertEquals(2, loaded.getMeta().getYieldCount());

        assertNotNull(loaded.getStorage());
        assertEquals("2 days", loaded.getStorage().getRefrigerator().getDuration());
        assertEquals(Boolean.FALSE, loaded.getStorage().getDoesNotKeep());

        assertNotNull(loaded.getNutrition());
        assertNotNull(loaded.getNutrition().getPerServing());
        assertEquals(420.5, loaded.getNutrition().getPerServing().getCalories());
        assertEquals(List.of("USDA", "Lab"), loaded.getNutrition().getSources());

        assertEquals(List.of("quick", "vegetarian"), loaded.getTags());
        assertEquals(List.of("Add salt gradually"), loaded.getChefNotes());

        assertNotNull(loaded.getDietary());
        assertEquals(List.of("vegetarian"), loaded.getDietary().getFlags());
        assertEquals(List.of("vegan"), loaded.getDietary().getNotSuitableFor());

        assertNotNull(loaded.getEquipment());
        assertEquals(1, loaded.getEquipment().size());
        assertEquals("Pot", loaded.getEquipment().get(0).getName());

        assertNotNull(loaded.getIngredients());
        assertEquals(1, loaded.getIngredients().size());
        assertEquals("Sauce", loaded.getIngredients().get(0).getGroupName());
        assertEquals(1, loaded.getIngredients().get(0).getItems().size());
        assertEquals(List.of("basil"), loaded.getIngredients().get(0).getItems().get(0).getSubstitutions());

        assertNotNull(loaded.getInstructions());
        assertEquals(1, loaded.getInstructions().size());
        assertEquals(Integer.valueOf(1), loaded.getInstructions().get(0).getStepNumber());
        assertEquals("Saute", loaded.getInstructions().get(0).getStructured().getAction());
        assertEquals(Integer.valueOf(160), loaded.getInstructions().get(0).getStructured().getTemperature().getCelsius());
        assertEquals(List.of("Do not burn garlic"), loaded.getInstructions().get(0).getTips());

        assertNotNull(loaded.getTroubleshooting());
        assertEquals(1, loaded.getTroubleshooting().size());
        assertEquals("Too dry", loaded.getTroubleshooting().get(0).getSymptom());
    }

    @Test
    void shouldPreserveInputOrderAfterTrimAndDeduplicateIds() {
        // Arrange
        recipeRepo.saveRecipe(minimalRecipe("recipe-a", "A"));
        recipeRepo.saveRecipe(minimalRecipe("recipe-b", "B"));

        // Act
        List<Recipe> result = recipeRepo.getRecipesByIds(List.of(" recipe-b ", "recipe-a", "recipe-b", " "));

        // Assert
        assertEquals(2, result.size());
        assertEquals("recipe-b", result.get(0).getId());
        assertEquals("recipe-a", result.get(1).getId());
    }

    @Test
    void shouldReplaceExistingRecipeWhenSavingSameIdAgain() {
        // Arrange
        recipeRepo.saveRecipe(minimalRecipe("recipe-overwrite", "Old Name"));
        Recipe updated = minimalRecipe("recipe-overwrite", "New Name");
        updated.setTags(List.of("updated-tag"));

        // Act
        recipeRepo.saveRecipe(updated);
        Optional<Recipe> result = recipeRepo.getRecipeById("recipe-overwrite");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getName());
        assertEquals(List.of("updated-tag"), result.get().getTags());
    }

    @Test
    void shouldReturnEmptyOptionalWhenRecipeDoesNotExist() {
        // Arrange

        // Act
        Optional<Recipe> result = recipeRepo.getRecipeById("missing-id");

        // Assert
        assertFalse(result.isPresent());
    }

    private Recipe fullRecipe(final String id) {
        Recipe recipe = minimalRecipe(id, "Tomato Pasta");

        RecipeMeta meta = new RecipeMeta();
        meta.setActiveTime("20m");
        meta.setPassiveTime("15m");
        meta.setTotalTime("35m");
        meta.setOvernightRequired(Boolean.FALSE);
        meta.setYields("2 servings");
        meta.setYieldCount(2);
        meta.setServingSizeG(350.0);
        recipe.setMeta(meta);

        Storage storage = new Storage();
        storage.setRefrigerator(new Storage.StorageCondition("2 days", "airtight"));
        storage.setFreezer(new Storage.StorageCondition("1 month", "freeze portions"));
        storage.setReheating("Stovetop");
        storage.setDoesNotKeep(Boolean.FALSE);
        recipe.setStorage(storage);

        Nutrition.PerServing perServing = new Nutrition.PerServing();
        perServing.setCalories(420.5);
        perServing.setProteinG(14.0);
        perServing.setCarbohydratesG(62.0);
        Nutrition nutrition = new Nutrition();
        nutrition.setPerServing(perServing);
        nutrition.setSources(List.of("USDA", "Lab"));
        recipe.setNutrition(nutrition);

        Dietary dietary = new Dietary();
        dietary.setFlags(List.of("vegetarian"));
        dietary.setNotSuitableFor(List.of("vegan"));
        recipe.setDietary(dietary);

        Equipment equipment = new Equipment();
        equipment.setName("Pot");
        equipment.setRequired(Boolean.TRUE);
        equipment.setAlternative("Pan");
        recipe.setEquipment(List.of(equipment));

        Ingredient ingredient = new Ingredient();
        ingredient.setName("Tomato");
        ingredient.setQuantity(2.0);
        ingredient.setUnit("pcs");
        ingredient.setPreparation("diced");
        ingredient.setNotes("ripe");
        ingredient.setIngredientId("ing-1");
        ingredient.setNutritionSource("USDA");
        ingredient.setSubstitutions(List.of("basil"));

        IngredientGroup group = new IngredientGroup();
        group.setGroupName("Sauce");
        group.setItems(List.of(ingredient));
        recipe.setIngredients(List.of(group));

        StructuredStep structured = new StructuredStep();
        structured.setAction("Saute");
        structured.setDuration("5m");
        structured.setTemperature(new StructuredStep.Temperature(160, 320));
        structured.setDonenessCues(new StructuredStep.DonenessCues("golden", "soft"));

        Instruction instruction = new Instruction();
        instruction.setStepNumber(1);
        instruction.setPhase("Cooking");
        instruction.setText("Cook onions and garlic");
        instruction.setStructured(structured);
        instruction.setTips(List.of("Do not burn garlic"));
        recipe.setInstructions(List.of(instruction));

        Troubleshooting troubleshooting = new Troubleshooting();
        troubleshooting.setSymptom("Too dry");
        troubleshooting.setLikelyCause("Overcooked");
        troubleshooting.setPrevention("Stir often");
        troubleshooting.setFix("Add water");
        recipe.setTroubleshooting(List.of(troubleshooting));

        return recipe;
    }

    private Recipe minimalRecipe(final String id, final String name) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setDescription("Simple description");
        recipe.setCategory("Dinner");
        recipe.setCuisine("Italian");
        recipe.setDifficulty("Easy");
        recipe.setCulturalContext("Family style");
        recipe.setTags(List.of("quick", "vegetarian"));
        recipe.setChefNotes(List.of("Add salt gradually"));
        return recipe;
    }
}

