package com.st20313779.fixtures;

import com.st20313779.model.recipe.Recipe;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Test fixture builder for Recipe objects following Builder Pattern.
 * Supports Equivalence Partitioning and Boundary Value Analysis test data creation.
 *
 * RTM Mapping: TR-001 (Recipe creation and persistence)
 */
public class TestRecipeBuilder {
    private String id;
    private String name;
    private String description;
    private String category;
    private String cuisine;
    private String difficulty;

    public TestRecipeBuilder() {
        this.id = UUID.randomUUID().toString();
        this.name = "Test Recipe";
        this.category = "Main Course";
        this.cuisine = "Italian";
        this.difficulty = "Easy";
    }

    public static TestRecipeBuilder aRecipe() {
        return new TestRecipeBuilder();
    }

    public TestRecipeBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public TestRecipeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TestRecipeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TestRecipeBuilder withMinimalData() {
        this.name = "X";
        this.description = null;
        this.category = null;
        this.cuisine = null;
        return this;
    }

    public TestRecipeBuilder withBlankName() {
        this.name = "   ";
        return this;
    }

    public TestRecipeBuilder withNameLength(int length) {
        this.name = "A".repeat(Math.max(1, length));
        return this;
    }

    public Recipe build() {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setCategory(category);
        recipe.setCuisine(cuisine);
        recipe.setDifficulty(difficulty);
        recipe.setTags(new ArrayList<>());
        recipe.setChefNotes(new ArrayList<>());
        return recipe;
    }
}

