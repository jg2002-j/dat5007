package com.st20313779.model;

import com.st20313779.model.recipe.Recipe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DayPlan {
    private LocalDate date;
    private Map<MealSlot, List<Recipe>> meals;
}
