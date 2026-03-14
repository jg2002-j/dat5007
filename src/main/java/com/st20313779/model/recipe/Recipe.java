package com.st20313779.model.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Recipe {
    private String id;
    private String name;
    private String description;
    private String category;
    private String cuisine;
    private String difficulty;
    private List<String> tags;
    private RecipeMeta meta;
    private Dietary dietary;
    private Storage storage;
    private List<Equipment> equipment;
    private List<IngredientGroup> ingredients;
    private List<Instruction> instructions;
    private List<Troubleshooting> troubleshooting;

    @JsonProperty("chef_notes")
    private List<String> chefNotes;

    @JsonProperty("cultural_context")
    private String culturalContext;

    private Nutrition nutrition;
}

