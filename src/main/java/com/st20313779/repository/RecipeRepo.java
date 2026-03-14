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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RecipeRepo {

    @Inject
    DataSource ds;

    @Transactional
    public void saveRecipe(final Recipe recipe) {
        if (recipe == null || recipe.getId() == null || recipe.getId().isBlank()) {
            throw new IllegalArgumentException("recipe.id is required");
        }

        final String recipeId = recipe.getId();

        try (final Connection conn = ds.getConnection()) {
            try (final PreparedStatement deletePs = conn.prepareStatement("delete from public.recipe where recipe_id = ?")) {
                deletePs.setString(1, recipeId);
                deletePs.executeUpdate();
            }

            try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe (recipe_id, name, description, category, cuisine, difficulty, cultural_context) values (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, recipeId);
                ps.setString(2, recipe.getName());
                ps.setString(3, recipe.getDescription());
                ps.setString(4, recipe.getCategory());
                ps.setString(5, recipe.getCuisine());
                ps.setString(6, recipe.getDifficulty());
                ps.setString(7, recipe.getCulturalContext());
                ps.executeUpdate();
            }

            insertRecipeMeta(conn, recipeId, recipe.getMeta());
            insertStorage(conn, recipeId, recipe.getStorage());
            insertNutrition(conn, recipeId, recipe.getNutrition());
            insertIndexedStringList(conn, "insert into public.recipe_tag (recipe_id, position, tag) values (?, ?, ?)", recipeId, recipe.getTags());
            insertIndexedStringList(conn, "insert into public.recipe_chef_note (recipe_id, position, note) values (?, ?, ?)", recipeId, recipe.getChefNotes());
            insertDietary(conn, recipeId, recipe.getDietary());
            insertEquipment(conn, recipeId, recipe.getEquipment());
            insertIngredients(conn, recipeId, recipe.getIngredients());
            insertInstructions(conn, recipeId, recipe.getInstructions());
            insertTroubleshooting(conn, recipeId, recipe.getTroubleshooting());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist recipe " + recipeId, e);
        }
    }

    public List<Recipe> fetchRecipes(final List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }

        final List<String> orderedIds = new ArrayList<>();
        for (final String uuid : uuids) {
            if (uuid == null) {
                continue;
            }
            final String trimmed = uuid.trim();
            if (!trimmed.isEmpty()) {
                orderedIds.add(trimmed);
            }
        }
        if (orderedIds.isEmpty()) {
            return List.of();
        }

        final List<String> distinctIds = new ArrayList<>(new LinkedHashSet<>(orderedIds));
        final String placeholders = String.join(",", java.util.Collections.nCopies(distinctIds.size(), "?"));
        final String sql = "select recipe_id, name, description, category, cuisine, difficulty, cultural_context from public.recipe where recipe_id in (" + placeholders + ")";

        final Map<String, Recipe> recipeById = new HashMap<>();
        try (final Connection conn = ds.getConnection();
             final PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (final String id : distinctIds) {
                ps.setString(idx++, id);
            }

            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe recipe = new Recipe();
                    recipe.setId(rs.getString("recipe_id"));
                    recipe.setName(rs.getString("name"));
                    recipe.setDescription(rs.getString("description"));
                    recipe.setCategory(rs.getString("category"));
                    recipe.setCuisine(rs.getString("cuisine"));
                    recipe.setDifficulty(rs.getString("difficulty"));
                    recipe.setCulturalContext(rs.getString("cultural_context"));
                    recipeById.put(recipe.getId(), recipe);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recipes", e);
        }

        final List<Recipe> recipes = new ArrayList<>();
        for (final String id : orderedIds) {
            final Recipe recipe = recipeById.get(id);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private void insertRecipeMeta(final Connection conn, final String recipeId, final RecipeMeta meta) throws SQLException {
        if (meta == null) {
            return;
        }
        try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe_meta (recipe_id, active_time, passive_time, total_time, overnight_required, yields, yield_count, serving_size_g) values (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, recipeId);
            ps.setString(2, meta.getActiveTime());
            ps.setString(3, meta.getPassiveTime());
            ps.setString(4, meta.getTotalTime());
            ps.setObject(5, meta.getOvernightRequired(), Types.BOOLEAN);
            ps.setString(6, meta.getYields());
            ps.setObject(7, meta.getYieldCount(), Types.INTEGER);
            ps.setObject(8, meta.getServingSizeG(), Types.DOUBLE);
            ps.executeUpdate();
        }
    }

    private void insertStorage(final Connection conn, final String recipeId, final Storage storage) throws SQLException {
        if (storage == null) {
            return;
        }
        try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe_storage (recipe_id, refrigerator_duration, refrigerator_notes, freezer_duration, freezer_notes, reheating, does_not_keep) values (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, recipeId);
            ps.setString(2, storage.getRefrigerator() == null ? null : storage.getRefrigerator().getDuration());
            ps.setString(3, storage.getRefrigerator() == null ? null : storage.getRefrigerator().getNotes());
            ps.setString(4, storage.getFreezer() == null ? null : storage.getFreezer().getDuration());
            ps.setString(5, storage.getFreezer() == null ? null : storage.getFreezer().getNotes());
            ps.setString(6, storage.getReheating());
            ps.setObject(7, storage.getDoesNotKeep(), Types.BOOLEAN);
            ps.executeUpdate();
        }
    }

    private void insertNutrition(final Connection conn, final String recipeId, final Nutrition nutrition) throws SQLException {
        if (nutrition == null) {
            return;
        }
        final Nutrition.PerServing perServing = nutrition.getPerServing();
        try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe_nutrition (recipe_id, calories, protein_g, carbohydrates_g, fat_g, saturated_fat_g, trans_fat_g, monounsaturated_fat_g, polyunsaturated_fat_g, fiber_g, sugar_g, sodium_mg, cholesterol_mg, potassium_mg, calcium_mg, iron_mg, magnesium_mg, phosphorus_mg, zinc_mg, vitamin_a_mcg, vitamin_c_mg, vitamin_d_mcg, vitamin_e_mg, vitamin_k_mcg, vitamin_b6_mg, vitamin_b12_mcg, thiamin_mg, riboflavin_mg, niacin_mg, folate_mcg, water_g, alcohol_g, caffeine_mg) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, recipeId);
            ps.setObject(2, perServing == null ? null : perServing.getCalories(), Types.DOUBLE);
            ps.setObject(3, perServing == null ? null : perServing.getProteinG(), Types.DOUBLE);
            ps.setObject(4, perServing == null ? null : perServing.getCarbohydratesG(), Types.DOUBLE);
            ps.setObject(5, perServing == null ? null : perServing.getFatG(), Types.DOUBLE);
            ps.setObject(6, perServing == null ? null : perServing.getSaturatedFatG(), Types.DOUBLE);
            ps.setObject(7, perServing == null ? null : perServing.getTransFatG(), Types.DOUBLE);
            ps.setObject(8, perServing == null ? null : perServing.getMonounsaturatedFatG(), Types.DOUBLE);
            ps.setObject(9, perServing == null ? null : perServing.getPolyunsaturatedFatG(), Types.DOUBLE);
            ps.setObject(10, perServing == null ? null : perServing.getFiberG(), Types.DOUBLE);
            ps.setObject(11, perServing == null ? null : perServing.getSugarG(), Types.DOUBLE);
            ps.setObject(12, perServing == null ? null : perServing.getSodiumMg(), Types.DOUBLE);
            ps.setObject(13, perServing == null ? null : perServing.getCholesterolMg(), Types.DOUBLE);
            ps.setObject(14, perServing == null ? null : perServing.getPotassiumMg(), Types.DOUBLE);
            ps.setObject(15, perServing == null ? null : perServing.getCalciumMg(), Types.DOUBLE);
            ps.setObject(16, perServing == null ? null : perServing.getIronMg(), Types.DOUBLE);
            ps.setObject(17, perServing == null ? null : perServing.getMagnesiumMg(), Types.DOUBLE);
            ps.setObject(18, perServing == null ? null : perServing.getPhosphorusMg(), Types.DOUBLE);
            ps.setObject(19, perServing == null ? null : perServing.getZincMg(), Types.DOUBLE);
            ps.setObject(20, perServing == null ? null : perServing.getVitaminAMcg(), Types.DOUBLE);
            ps.setObject(21, perServing == null ? null : perServing.getVitaminCMg(), Types.DOUBLE);
            ps.setObject(22, perServing == null ? null : perServing.getVitaminDMcg(), Types.DOUBLE);
            ps.setObject(23, perServing == null ? null : perServing.getVitaminEMg(), Types.DOUBLE);
            ps.setObject(24, perServing == null ? null : perServing.getVitaminKMcg(), Types.DOUBLE);
            ps.setObject(25, perServing == null ? null : perServing.getVitaminB6Mg(), Types.DOUBLE);
            ps.setObject(26, perServing == null ? null : perServing.getVitaminB12Mcg(), Types.DOUBLE);
            ps.setObject(27, perServing == null ? null : perServing.getThiaminMg(), Types.DOUBLE);
            ps.setObject(28, perServing == null ? null : perServing.getRiboflavinMg(), Types.DOUBLE);
            ps.setObject(29, perServing == null ? null : perServing.getNiacinMg(), Types.DOUBLE);
            ps.setObject(30, perServing == null ? null : perServing.getFolateMcg(), Types.DOUBLE);
            ps.setObject(31, perServing == null ? null : perServing.getWaterG(), Types.DOUBLE);
            ps.setObject(32, perServing == null ? null : perServing.getAlcoholG(), Types.DOUBLE);
            ps.setObject(33, perServing == null ? null : perServing.getCaffeineMg(), Types.DOUBLE);
            ps.executeUpdate();
        }
        insertIndexedStringList(conn, "insert into public.recipe_nutrition_source (recipe_id, position, source) values (?, ?, ?)", recipeId, nutrition.getSources());
    }

    private void insertDietary(final Connection conn, final String recipeId, final Dietary dietary) throws SQLException {
        if (dietary == null) {
            return;
        }
        insertIndexedStringList(conn, "insert into public.recipe_dietary_flag (recipe_id, position, flag) values (?, ?, ?)", recipeId, dietary.getFlags());
        insertIndexedStringList(conn, "insert into public.recipe_dietary_not_suitable (recipe_id, position, item) values (?, ?, ?)", recipeId, dietary.getNotSuitableFor());
    }

    private void insertEquipment(final Connection conn, final String recipeId, final List<Equipment> equipment) throws SQLException {
        if (equipment == null || equipment.isEmpty()) {
            return;
        }
        try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe_equipment (recipe_id, position, name, required, alternative) values (?, ?, ?, ?, ?)")) {
            int position = 0;
            for (final Equipment item : equipment) {
                ps.setString(1, recipeId);
                ps.setInt(2, position++);
                ps.setString(3, item == null ? null : item.getName());
                ps.setObject(4, item == null ? null : item.getRequired(), Types.BOOLEAN);
                ps.setString(5, item == null ? null : item.getAlternative());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertIngredients(final Connection conn, final String recipeId, final List<IngredientGroup> groups) throws SQLException {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        try (
                final PreparedStatement groupPs = conn.prepareStatement("insert into public.recipe_ingredient_group (recipe_id, position, group_name) values (?, ?, ?)");
                final PreparedStatement ingredientPs = conn.prepareStatement("insert into public.recipe_ingredient (recipe_id, group_position, position, name, quantity, unit, preparation, notes, ingredient_id, nutrition_source) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                final PreparedStatement substitutionPs = conn.prepareStatement("insert into public.recipe_ingredient_substitution (recipe_id, group_position, ingredient_position, position, substitution) values (?, ?, ?, ?, ?)")
        ) {
            int groupPosition = 0;
            for (final IngredientGroup group : groups) {
                groupPs.setString(1, recipeId);
                groupPs.setInt(2, groupPosition);
                groupPs.setString(3, group == null ? null : group.getGroupName());
                groupPs.addBatch();

                int ingredientPosition = 0;
                for (final Ingredient ingredient : safeList(group == null ? null : group.getItems())) {
                    ingredientPs.setString(1, recipeId);
                    ingredientPs.setInt(2, groupPosition);
                    ingredientPs.setInt(3, ingredientPosition);
                    ingredientPs.setString(4, ingredient == null ? null : ingredient.getName());
                    ingredientPs.setObject(5, ingredient == null ? null : ingredient.getQuantity(), Types.DOUBLE);
                    ingredientPs.setString(6, ingredient == null ? null : ingredient.getUnit());
                    ingredientPs.setString(7, ingredient == null ? null : ingredient.getPreparation());
                    ingredientPs.setString(8, ingredient == null ? null : ingredient.getNotes());
                    ingredientPs.setString(9, ingredient == null ? null : ingredient.getIngredientId());
                    ingredientPs.setString(10, ingredient == null ? null : ingredient.getNutritionSource());
                    ingredientPs.addBatch();

                    int substitutionPosition = 0;
                    for (final String substitution : safeList(ingredient == null ? null : ingredient.getSubstitutions())) {
                        substitutionPs.setString(1, recipeId);
                        substitutionPs.setInt(2, groupPosition);
                        substitutionPs.setInt(3, ingredientPosition);
                        substitutionPs.setInt(4, substitutionPosition++);
                        substitutionPs.setString(5, substitution);
                        substitutionPs.addBatch();
                    }

                    ingredientPosition++;
                }
                groupPosition++;
            }
            groupPs.executeBatch();
            ingredientPs.executeBatch();
            substitutionPs.executeBatch();
        }
    }

    private void insertInstructions(final Connection conn, final String recipeId, final List<Instruction> instructions) throws SQLException {
        if (instructions == null || instructions.isEmpty()) {
            return;
        }

        try (
                final PreparedStatement instructionPs = conn.prepareStatement("insert into public.recipe_instruction (recipe_id, position, step_number, phase, text, structured_action, structured_duration, temperature_celsius, temperature_fahrenheit, doneness_visual, doneness_tactile) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                final PreparedStatement tipPs = conn.prepareStatement("insert into public.recipe_instruction_tip (recipe_id, instruction_position, position, tip) values (?, ?, ?, ?)")
        ) {
            int position = 0;
            for (final Instruction instruction : instructions) {
                final StructuredStep structured = instruction == null ? null : instruction.getStructured();
                instructionPs.setString(1, recipeId);
                instructionPs.setInt(2, position);
                instructionPs.setObject(3, instruction == null ? null : instruction.getStepNumber(), Types.INTEGER);
                instructionPs.setString(4, instruction == null ? null : instruction.getPhase());
                instructionPs.setString(5, instruction == null ? null : instruction.getText());
                instructionPs.setString(6, structured == null ? null : structured.getAction());
                instructionPs.setString(7, structured == null ? null : structured.getDuration());
                instructionPs.setObject(8, structured == null || structured.getTemperature() == null ? null : structured.getTemperature().getCelsius(), Types.INTEGER);
                instructionPs.setObject(9, structured == null || structured.getTemperature() == null ? null : structured.getTemperature().getFahrenheit(), Types.INTEGER);
                instructionPs.setString(10, structured == null || structured.getDonenessCues() == null ? null : structured.getDonenessCues().getVisual());
                instructionPs.setString(11, structured == null || structured.getDonenessCues() == null ? null : structured.getDonenessCues().getTactile());
                instructionPs.addBatch();

                int tipPosition = 0;
                for (final String tip : safeList(instruction == null ? null : instruction.getTips())) {
                    tipPs.setString(1, recipeId);
                    tipPs.setInt(2, position);
                    tipPs.setInt(3, tipPosition++);
                    tipPs.setString(4, tip);
                    tipPs.addBatch();
                }

                position++;
            }
            instructionPs.executeBatch();
            tipPs.executeBatch();
        }
    }

    private void insertTroubleshooting(final Connection conn, final String recipeId, final List<Troubleshooting> troubleshooting) throws SQLException {
        if (troubleshooting == null || troubleshooting.isEmpty()) {
            return;
        }
        try (final PreparedStatement ps = conn.prepareStatement("insert into public.recipe_troubleshooting (recipe_id, position, symptom, likely_cause, prevention, fix) values (?, ?, ?, ?, ?, ?)")) {
            int position = 0;
            for (final Troubleshooting item : troubleshooting) {
                ps.setString(1, recipeId);
                ps.setInt(2, position++);
                ps.setString(3, item == null ? null : item.getSymptom());
                ps.setString(4, item == null ? null : item.getLikelyCause());
                ps.setString(5, item == null ? null : item.getPrevention());
                ps.setString(6, item == null ? null : item.getFix());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertIndexedStringList(final Connection conn, final String sql, final String recipeId, final List<String> values) throws SQLException {
        if (values == null || values.isEmpty()) {
            return;
        }
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            int position = 0;
            for (final String value : values) {
                ps.setString(1, recipeId);
                ps.setInt(2, position++);
                ps.setString(3, value);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private <T> List<T> safeList(final List<T> list) {
        return list == null ? List.of() : list;
    }
}

