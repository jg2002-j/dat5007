package com.st20313779.repository;

import com.st20313779.model.MealSlot;
import com.st20313779.model.recipe.Recipe;
import com.st20313779.repository.dto.DayMealsDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class PlannerRepo {

    @Inject
    DataSource ds;

    @Transactional
    public void saveDayPlanBatch(final Date date, final Map<MealSlot, List<Recipe>> meals) {
        try (final Connection conn = ds.getConnection()) {
            // 1. Delete existing entries for this date to allow overrides
            try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM public.meals WHERE tdate = ?")) {
                deletePs.setDate(1, date);
                deletePs.executeUpdate();
            }

            // 2. Batch Insert
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO public.meals (tdate, slot, recipe_uuid) VALUES (?, ?, ?)")) {
                for (Map.Entry<MealSlot, List<Recipe>> entry : meals.entrySet()) {
                    for (Recipe recipe : entry.getValue()) {
                        ps.setDate(1, date);
                        ps.setString(2, entry.getKey().name());
                        ps.setString(3, recipe.getId());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during batch save", e);
        }
    }

    @Transactional
    public void saveSlotMealUuid(final Date date, final MealSlot slot, final String uuid) {
        try (
                final Connection conn = ds.getConnection();
                final PreparedStatement ps = conn.prepareStatement("insert into public.meals (tdate, slot, recipe_uuid) values (?, ?, ?)")
        ) {
            ps.setDate(1, date);
            ps.setString(2, String.valueOf(slot));
            ps.setString(3, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

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
                    final List<String> recipes = meals.getMealUuids().computeIfAbsent(slot, ignored -> new ArrayList<>());
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
