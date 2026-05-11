-- Seed recipes for E2E testing (Matches the UUIDs used in Cypress)
INSERT INTO public.recipe (recipe_id, name, category, cuisine, difficulty)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'E2E Pasta', 'Main', 'Italian', 'Easy');

INSERT INTO public.recipe (recipe_id, name, category, cuisine, difficulty)
VALUES ('550e8400-e29b-41d4-a716-446655440002', 'E2E Salad', 'Starter', 'French', 'Easy');