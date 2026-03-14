package com.st20313779.repository;

import com.st20313779.model.MealSlot;
import com.st20313779.repository.dto.DayMealsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.List;

@Slf4j
@ApplicationScoped
public class PlannerRepo {

    @Inject
    DataSource ds;

    @Transactional
    public DayMealsDto getDayPlan(final Date date) {
        try (
                final Connection conn = ds.getConnection();
                final PreparedStatement ps = conn.prepareStatement("select * from public.meals where tdate = ?")
        ) {
            ps.setDate(1, date);
            try (final ResultSet rs = ps.executeQuery()) {
                final DayMealsDto meals = new DayMealsDto()
                        .setDate(date)
                        .setMealUuids(new HashMap<>());
                while (rs.next()) {
                    final MealSlot slot = rs.getString("slot") != null ? MealSlot.valueOf(rs.getString("slot")) : MealSlot.OTHER;
                    final String uuid = rs.getString("recipe_uuid");
                    final List<String> recipes = meals.getMealUuids().getOrDefault(slot, List.of());
                    recipes.add(uuid);
                    meals.getMealUuids().put(slot, recipes);
                }
                return meals;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
