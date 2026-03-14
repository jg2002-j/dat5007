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
public class Nutrition {
    @JsonProperty("per_serving")
    private PerServing perServing;

    private List<String> sources;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PerServing {
        private Double calories;

        @JsonProperty("protein_g")
        private Double proteinG;

        @JsonProperty("carbohydrates_g")
        private Double carbohydratesG;

        @JsonProperty("fat_g")
        private Double fatG;

        @JsonProperty("saturated_fat_g")
        private Double saturatedFatG;

        @JsonProperty("trans_fat_g")
        private Double transFatG;

        @JsonProperty("monounsaturated_fat_g")
        private Double monounsaturatedFatG;

        @JsonProperty("polyunsaturated_fat_g")
        private Double polyunsaturatedFatG;

        @JsonProperty("fiber_g")
        private Double fiberG;

        @JsonProperty("sugar_g")
        private Double sugarG;

        @JsonProperty("sodium_mg")
        private Double sodiumMg;

        @JsonProperty("cholesterol_mg")
        private Double cholesterolMg;

        @JsonProperty("potassium_mg")
        private Double potassiumMg;

        @JsonProperty("calcium_mg")
        private Double calciumMg;

        @JsonProperty("iron_mg")
        private Double ironMg;

        @JsonProperty("magnesium_mg")
        private Double magnesiumMg;

        @JsonProperty("phosphorus_mg")
        private Double phosphorusMg;

        @JsonProperty("zinc_mg")
        private Double zincMg;

        @JsonProperty("vitamin_a_mcg")
        private Double vitaminAMcg;

        @JsonProperty("vitamin_c_mg")
        private Double vitaminCMg;

        @JsonProperty("vitamin_d_mcg")
        private Double vitaminDMcg;

        @JsonProperty("vitamin_e_mg")
        private Double vitaminEMg;

        @JsonProperty("vitamin_k_mcg")
        private Double vitaminKMcg;

        @JsonProperty("vitamin_b6_mg")
        private Double vitaminB6Mg;

        @JsonProperty("vitamin_b12_mcg")
        private Double vitaminB12Mcg;

        @JsonProperty("thiamin_mg")
        private Double thiaminMg;

        @JsonProperty("riboflavin_mg")
        private Double riboflavinMg;

        @JsonProperty("niacin_mg")
        private Double niacinMg;

        @JsonProperty("folate_mcg")
        private Double folateMcg;

        @JsonProperty("water_g")
        private Double waterG;

        @JsonProperty("alcohol_g")
        private Double alcoholG;

        @JsonProperty("caffeine_mg")
        private Double caffeineMg;
    }
}

