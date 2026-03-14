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
public class RecipeListItem {
    private String id;
    private String name;
    private String description;
    private String category;
    private String cuisine;
    private String difficulty;
    private List<String> tags;
    private RecipeMeta meta;
    private Dietary dietary;

    @JsonProperty("nutrition_summary")
    private NutritionSummary nutritionSummary;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NutritionSummary {
        private Double calories;

        @JsonProperty("protein_g")
        private Double proteinG;

        @JsonProperty("carbohydrates_g")
        private Double carbohydratesG;

        @JsonProperty("fat_g")
        private Double fatG;
    }
}

