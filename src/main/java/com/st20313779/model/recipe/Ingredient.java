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
public class Ingredient {
    private String name;
    private Double quantity;
    private String unit;
    private String preparation;
    private String notes;
    private List<String> substitutions;

    @JsonProperty("ingredient_id")
    private String ingredientId;

    @JsonProperty("nutrition_source")
    private String nutritionSource;
}

