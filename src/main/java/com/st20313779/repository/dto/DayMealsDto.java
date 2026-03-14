package com.st20313779.repository.dto;

import com.st20313779.model.MealSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DayMealsDto {
    private Date date;
    private Map<MealSlot, List<String>> mealUuids;
}
