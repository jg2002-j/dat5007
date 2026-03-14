package com.st20313779.repository;

import com.st20313779.model.recipe.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public Optional<Recipe> getRecipeById(final String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("uuid is required");
        }
        return getRecipesByIds(List.of(uuid.trim())).stream().findFirst();
    }

    public List<Recipe> getRecipesByIds(final List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }

        // Preserve order, deduplicate, drop blanks
        final List<String> orderedIds = uuids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new))
                .stream().toList();

        if (orderedIds.isEmpty()) {
            return List.of();
        }

        try (final Connection conn = ds.getConnection()) {
            final Map<String, Recipe> byId = fetchBaseRecipes(conn, orderedIds);
            if (byId.isEmpty()) {
                return List.of();
            }
            populateMeta(conn, orderedIds, byId);
            populateStorage(conn, orderedIds, byId);
            populateNutrition(conn, orderedIds, byId);
            populateTags(conn, orderedIds, byId);
            populateChefNotes(conn, orderedIds, byId);
            populateDietary(conn, orderedIds, byId);
            populateEquipment(conn, orderedIds, byId);
            populateIngredients(conn, orderedIds, byId);
            populateInstructions(conn, orderedIds, byId);
            populateTroubleshooting(conn, orderedIds, byId);

            return orderedIds.stream()
                    .filter(byId::containsKey)
                    .map(byId::get)
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recipes", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String placeholders(final int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    private void bindIds(final PreparedStatement ps, final List<String> ids, final int startIndex) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setString(startIndex + i, ids.get(i));
        }
    }

    private Map<String, Recipe> fetchBaseRecipes(final Connection conn, final List<String> ids) throws SQLException {
        final String sql = "select recipe_id, name, description, category, cuisine, difficulty, cultural_context " +
                           "from public.recipe where recipe_id in (" + placeholders(ids.size()) + ")";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                final Map<String, Recipe> byId = new HashMap<>();
                while (rs.next()) {
                    final Recipe r = new Recipe();
                    r.setId(rs.getString("recipe_id"));
                    r.setName(rs.getString("name"));
                    r.setDescription(rs.getString("description"));
                    r.setCategory(rs.getString("category"));
                    r.setCuisine(rs.getString("cuisine"));
                    r.setDifficulty(rs.getString("difficulty"));
                    r.setCulturalContext(rs.getString("cultural_context"));
                    byId.put(r.getId(), r);
                }
                return byId;
            }
        }
    }

    private void populateMeta(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, active_time, passive_time, total_time, overnight_required, yields, yield_count, serving_size_g " +
                           "from public.recipe_meta where recipe_id in (" + placeholders(ids.size()) + ")";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    final RecipeMeta meta = new RecipeMeta();
                    meta.setActiveTime(rs.getString("active_time"));
                    meta.setPassiveTime(rs.getString("passive_time"));
                    meta.setTotalTime(rs.getString("total_time"));
                    final boolean overnight = rs.getBoolean("overnight_required");
                    meta.setOvernightRequired(rs.wasNull() ? null : overnight);
                    meta.setYields(rs.getString("yields"));
                    final int yieldCount = rs.getInt("yield_count");
                    meta.setYieldCount(rs.wasNull() ? null : yieldCount);
                    final double servingSize = rs.getDouble("serving_size_g");
                    meta.setServingSizeG(rs.wasNull() ? null : servingSize);
                    r.setMeta(meta);
                }
            }
        }
    }

    private void populateStorage(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, refrigerator_duration, refrigerator_notes, freezer_duration, freezer_notes, reheating, does_not_keep " +
                           "from public.recipe_storage where recipe_id in (" + placeholders(ids.size()) + ")";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    final Storage storage = new Storage();
                    final String refDuration = rs.getString("refrigerator_duration");
                    final String refNotes = rs.getString("refrigerator_notes");
                    if (refDuration != null || refNotes != null) {
                        storage.setRefrigerator(new Storage.StorageCondition(refDuration, refNotes));
                    }
                    final String frzDuration = rs.getString("freezer_duration");
                    final String frzNotes = rs.getString("freezer_notes");
                    if (frzDuration != null || frzNotes != null) {
                        storage.setFreezer(new Storage.StorageCondition(frzDuration, frzNotes));
                    }
                    storage.setReheating(rs.getString("reheating"));
                    final boolean doesNotKeep = rs.getBoolean("does_not_keep");
                    storage.setDoesNotKeep(rs.wasNull() ? null : doesNotKeep);
                    r.setStorage(storage);
                }
            }
        }
    }

    private void populateNutrition(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, calories, protein_g, carbohydrates_g, fat_g, saturated_fat_g, trans_fat_g, monounsaturated_fat_g, polyunsaturated_fat_g, fiber_g, sugar_g, sodium_mg, cholesterol_mg, potassium_mg, calcium_mg, iron_mg, magnesium_mg, phosphorus_mg, zinc_mg, vitamin_a_mcg, vitamin_c_mg, vitamin_d_mcg, vitamin_e_mg, vitamin_k_mcg, vitamin_b6_mg, vitamin_b12_mcg, thiamin_mg, riboflavin_mg, niacin_mg, folate_mcg, water_g, alcohol_g, caffeine_mg " +
                           "from public.recipe_nutrition where recipe_id in (" + placeholders(ids.size()) + ")";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    final Nutrition.PerServing p = new Nutrition.PerServing();
                    p.setCalories(nullable(rs, "calories"));
                    p.setProteinG(nullable(rs, "protein_g"));
                    p.setCarbohydratesG(nullable(rs, "carbohydrates_g"));
                    p.setFatG(nullable(rs, "fat_g"));
                    p.setSaturatedFatG(nullable(rs, "saturated_fat_g"));
                    p.setTransFatG(nullable(rs, "trans_fat_g"));
                    p.setMonounsaturatedFatG(nullable(rs, "monounsaturated_fat_g"));
                    p.setPolyunsaturatedFatG(nullable(rs, "polyunsaturated_fat_g"));
                    p.setFiberG(nullable(rs, "fiber_g"));
                    p.setSugarG(nullable(rs, "sugar_g"));
                    p.setSodiumMg(nullable(rs, "sodium_mg"));
                    p.setCholesterolMg(nullable(rs, "cholesterol_mg"));
                    p.setPotassiumMg(nullable(rs, "potassium_mg"));
                    p.setCalciumMg(nullable(rs, "calcium_mg"));
                    p.setIronMg(nullable(rs, "iron_mg"));
                    p.setMagnesiumMg(nullable(rs, "magnesium_mg"));
                    p.setPhosphorusMg(nullable(rs, "phosphorus_mg"));
                    p.setZincMg(nullable(rs, "zinc_mg"));
                    p.setVitaminAMcg(nullable(rs, "vitamin_a_mcg"));
                    p.setVitaminCMg(nullable(rs, "vitamin_c_mg"));
                    p.setVitaminDMcg(nullable(rs, "vitamin_d_mcg"));
                    p.setVitaminEMg(nullable(rs, "vitamin_e_mg"));
                    p.setVitaminKMcg(nullable(rs, "vitamin_k_mcg"));
                    p.setVitaminB6Mg(nullable(rs, "vitamin_b6_mg"));
                    p.setVitaminB12Mcg(nullable(rs, "vitamin_b12_mcg"));
                    p.setThiaminMg(nullable(rs, "thiamin_mg"));
                    p.setRiboflavinMg(nullable(rs, "riboflavin_mg"));
                    p.setNiacinMg(nullable(rs, "niacin_mg"));
                    p.setFolateMcg(nullable(rs, "folate_mcg"));
                    p.setWaterG(nullable(rs, "water_g"));
                    p.setAlcoholG(nullable(rs, "alcohol_g"));
                    p.setCaffeineMg(nullable(rs, "caffeine_mg"));
                    r.setNutrition(new Nutrition(p, new ArrayList<>()));
                }
            }
        }
        // Nutrition sources
        final String srcSql = "select recipe_id, source from public.recipe_nutrition_source " +
                              "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(srcSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null || r.getNutrition() == null) continue;
                    r.getNutrition().getSources().add(rs.getString("source"));
                }
            }
        }
    }

    private void populateTags(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, tag from public.recipe_tag " +
                           "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getTags() == null) r.setTags(new ArrayList<>());
                    r.getTags().add(rs.getString("tag"));
                }
            }
        }
    }

    private void populateChefNotes(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, note from public.recipe_chef_note " +
                           "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getChefNotes() == null) r.setChefNotes(new ArrayList<>());
                    r.getChefNotes().add(rs.getString("note"));
                }
            }
        }
    }

    private void populateDietary(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        // Flags
        final String flagSql = "select recipe_id, flag from public.recipe_dietary_flag " +
                               "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(flagSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getDietary() == null) r.setDietary(new Dietary());
                    if (r.getDietary().getFlags() == null) r.getDietary().setFlags(new ArrayList<>());
                    r.getDietary().getFlags().add(rs.getString("flag"));
                }
            }
        }
        // Not suitable for
        final String notSuitSql = "select recipe_id, item from public.recipe_dietary_not_suitable " +
                                  "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(notSuitSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getDietary() == null) r.setDietary(new Dietary());
                    if (r.getDietary().getNotSuitableFor() == null) r.getDietary().setNotSuitableFor(new ArrayList<>());
                    r.getDietary().getNotSuitableFor().add(rs.getString("item"));
                }
            }
        }
    }

    private void populateEquipment(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, name, required, alternative from public.recipe_equipment " +
                           "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getEquipment() == null) r.setEquipment(new ArrayList<>());
                    final Equipment eq = new Equipment();
                    eq.setName(rs.getString("name"));
                    final boolean required = rs.getBoolean("required");
                    eq.setRequired(rs.wasNull() ? null : required);
                    eq.setAlternative(rs.getString("alternative"));
                    r.getEquipment().add(eq);
                }
            }
        }
    }

    private void populateIngredients(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        // Groups: Map<recipeId, Map<groupPosition, IngredientGroup>>
        final Map<String, Map<Integer, IngredientGroup>> groupsByRecipe = new LinkedHashMap<>();
        final String groupSql = "select recipe_id, position, group_name from public.recipe_ingredient_group " +
                                "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(groupSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String recipeId = rs.getString("recipe_id");
                    final IngredientGroup group = new IngredientGroup();
                    group.setGroupName(rs.getString("group_name"));
                    group.setItems(new ArrayList<>());
                    groupsByRecipe.computeIfAbsent(recipeId, k -> new LinkedHashMap<>())
                                  .put(rs.getInt("position"), group);
                }
            }
        }

        // Ingredients: Map<recipeId, Map<groupPos, Map<ingredientPos, Ingredient>>>
        final Map<String, Map<Integer, Map<Integer, Ingredient>>> ingredientsByRecipe = new LinkedHashMap<>();
        final String ingSql = "select recipe_id, group_position, position, name, quantity, unit, preparation, notes, ingredient_id, nutrition_source " +
                              "from public.recipe_ingredient where recipe_id in (" + placeholders(ids.size()) + ") " +
                              "order by recipe_id, group_position, position";
        try (final PreparedStatement ps = conn.prepareStatement(ingSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String recipeId = rs.getString("recipe_id");
                    final int groupPos = rs.getInt("group_position");
                    final int pos = rs.getInt("position");
                    final Ingredient ing = new Ingredient();
                    ing.setName(rs.getString("name"));
                    final double qty = rs.getDouble("quantity");
                    ing.setQuantity(rs.wasNull() ? null : qty);
                    ing.setUnit(rs.getString("unit"));
                    ing.setPreparation(rs.getString("preparation"));
                    ing.setNotes(rs.getString("notes"));
                    ing.setIngredientId(rs.getString("ingredient_id"));
                    ing.setNutritionSource(rs.getString("nutrition_source"));
                    ing.setSubstitutions(new ArrayList<>());
                    ingredientsByRecipe.computeIfAbsent(recipeId, k -> new LinkedHashMap<>())
                                       .computeIfAbsent(groupPos, k -> new LinkedHashMap<>())
                                       .put(pos, ing);
                    // Attach to group
                    final Map<Integer, IngredientGroup> groups = groupsByRecipe.get(recipeId);
                    if (groups != null && groups.containsKey(groupPos)) {
                        groups.get(groupPos).getItems().add(ing);
                    }
                }
            }
        }

        // Substitutions
        final String subSql = "select recipe_id, group_position, ingredient_position, substitution " +
                              "from public.recipe_ingredient_substitution where recipe_id in (" + placeholders(ids.size()) + ") " +
                              "order by recipe_id, group_position, ingredient_position, position";
        try (final PreparedStatement ps = conn.prepareStatement(subSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String recipeId = rs.getString("recipe_id");
                    final Map<Integer, Map<Integer, Ingredient>> groups = ingredientsByRecipe.get(recipeId);
                    if (groups == null) continue;
                    final Map<Integer, Ingredient> ingredients = groups.get(rs.getInt("group_position"));
                    if (ingredients == null) continue;
                    final Ingredient ing = ingredients.get(rs.getInt("ingredient_position"));
                    if (ing == null) continue;
                    ing.getSubstitutions().add(rs.getString("substitution"));
                }
            }
        }

        // Set on recipes
        groupsByRecipe.forEach((recipeId, groupMap) -> {
            final Recipe r = byId.get(recipeId);
            if (r != null) r.setIngredients(new ArrayList<>(groupMap.values()));
        });
    }

    private void populateInstructions(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        // Map<recipeId, Map<position, Instruction>>
        final Map<String, Map<Integer, Instruction>> instructionsByRecipe = new LinkedHashMap<>();
        final String instrSql = "select recipe_id, position, step_number, phase, text, structured_action, structured_duration, " +
                                "temperature_celsius, temperature_fahrenheit, doneness_visual, doneness_tactile " +
                                "from public.recipe_instruction where recipe_id in (" + placeholders(ids.size()) + ") " +
                                "order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(instrSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String recipeId = rs.getString("recipe_id");
                    final Instruction instr = new Instruction();
                    final int stepNum = rs.getInt("step_number");
                    instr.setStepNumber(rs.wasNull() ? null : stepNum);
                    instr.setPhase(rs.getString("phase"));
                    instr.setText(rs.getString("text"));
                    instr.setTips(new ArrayList<>());
                    // Structured step
                    final String action = rs.getString("structured_action");
                    final String duration = rs.getString("structured_duration");
                    final int tempC = rs.getInt("temperature_celsius");
                    final Integer tempCVal = rs.wasNull() ? null : tempC;
                    final int tempF = rs.getInt("temperature_fahrenheit");
                    final Integer tempFVal = rs.wasNull() ? null : tempF;
                    final String visual = rs.getString("doneness_visual");
                    final String tactile = rs.getString("doneness_tactile");
                    if (action != null || duration != null || tempCVal != null || tempFVal != null || visual != null || tactile != null) {
                        final StructuredStep structured = new StructuredStep();
                        structured.setAction(action);
                        structured.setDuration(duration);
                        if (tempCVal != null || tempFVal != null) {
                            structured.setTemperature(new StructuredStep.Temperature(tempCVal, tempFVal));
                        }
                        if (visual != null || tactile != null) {
                            structured.setDonenessCues(new StructuredStep.DonenessCues(visual, tactile));
                        }
                        instr.setStructured(structured);
                    }
                    instructionsByRecipe.computeIfAbsent(recipeId, k -> new LinkedHashMap<>())
                                        .put(rs.getInt("position"), instr);
                }
            }
        }

        // Tips
        final String tipSql = "select recipe_id, instruction_position, tip from public.recipe_instruction_tip " +
                              "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, instruction_position, position";
        try (final PreparedStatement ps = conn.prepareStatement(tipSql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String recipeId = rs.getString("recipe_id");
                    final Map<Integer, Instruction> instructions = instructionsByRecipe.get(recipeId);
                    if (instructions == null) continue;
                    final Instruction instr = instructions.get(rs.getInt("instruction_position"));
                    if (instr == null) continue;
                    instr.getTips().add(rs.getString("tip"));
                }
            }
        }

        // Set on recipes
        instructionsByRecipe.forEach((recipeId, instrMap) -> {
            final Recipe r = byId.get(recipeId);
            if (r != null) r.setInstructions(new ArrayList<>(instrMap.values()));
        });
    }

    private void populateTroubleshooting(final Connection conn, final List<String> ids, final Map<String, Recipe> byId) throws SQLException {
        final String sql = "select recipe_id, symptom, likely_cause, prevention, fix from public.recipe_troubleshooting " +
                           "where recipe_id in (" + placeholders(ids.size()) + ") order by recipe_id, position";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids, 1);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Recipe r = byId.get(rs.getString("recipe_id"));
                    if (r == null) continue;
                    if (r.getTroubleshooting() == null) r.setTroubleshooting(new ArrayList<>());
                    final Troubleshooting t = new Troubleshooting();
                    t.setSymptom(rs.getString("symptom"));
                    t.setLikelyCause(rs.getString("likely_cause"));
                    t.setPrevention(rs.getString("prevention"));
                    t.setFix(rs.getString("fix"));
                    r.getTroubleshooting().add(t);
                }
            }
        }
    }

    /** Reads a nullable DOUBLE column, returning null if SQL NULL. */
    private Double nullable(final ResultSet rs, final String col) throws SQLException {
        final double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
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

