create table public.recipe (
    recipe_id varchar(255) primary key,
    name text,
    description text,
    category varchar(255),
    cuisine varchar(255),
    difficulty varchar(255),
    cultural_context text
);

create table public.recipe_meta (
    recipe_id varchar(255) primary key references public.recipe(recipe_id) on delete cascade,
    active_time varchar(255),
    passive_time varchar(255),
    total_time varchar(255),
    overnight_required boolean,
    yields varchar(255),
    yield_count integer,
    serving_size_g double precision
);

create table public.recipe_storage (
    recipe_id varchar(255) primary key references public.recipe(recipe_id) on delete cascade,
    refrigerator_duration varchar(255),
    refrigerator_notes text,
    freezer_duration varchar(255),
    freezer_notes text,
    reheating text,
    does_not_keep boolean
);

create table public.recipe_nutrition (
    recipe_id varchar(255) primary key references public.recipe(recipe_id) on delete cascade,
    calories double precision,
    protein_g double precision,
    carbohydrates_g double precision,
    fat_g double precision,
    saturated_fat_g double precision,
    trans_fat_g double precision,
    monounsaturated_fat_g double precision,
    polyunsaturated_fat_g double precision,
    fiber_g double precision,
    sugar_g double precision,
    sodium_mg double precision,
    cholesterol_mg double precision,
    potassium_mg double precision,
    calcium_mg double precision,
    iron_mg double precision,
    magnesium_mg double precision,
    phosphorus_mg double precision,
    zinc_mg double precision,
    vitamin_a_mcg double precision,
    vitamin_c_mg double precision,
    vitamin_d_mcg double precision,
    vitamin_e_mg double precision,
    vitamin_k_mcg double precision,
    vitamin_b6_mg double precision,
    vitamin_b12_mcg double precision,
    thiamin_mg double precision,
    riboflavin_mg double precision,
    niacin_mg double precision,
    folate_mcg double precision,
    water_g double precision,
    alcohol_g double precision,
    caffeine_mg double precision
);

create table public.recipe_tag (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    tag text,
    primary key (recipe_id, position)
);

create table public.recipe_chef_note (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    note text,
    primary key (recipe_id, position)
);

create table public.recipe_dietary_flag (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    flag text,
    primary key (recipe_id, position)
);

create table public.recipe_dietary_not_suitable (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    item text,
    primary key (recipe_id, position)
);

create table public.recipe_equipment (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    name varchar(255),
    required boolean,
    alternative text,
    primary key (recipe_id, position)
);

create table public.recipe_ingredient_group (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    group_name varchar(255),
    primary key (recipe_id, position)
);

create table public.recipe_ingredient (
    recipe_id varchar(255) not null,
    group_position integer not null,
    position integer not null,
    name varchar(255),
    quantity double precision,
    unit varchar(255),
    preparation text,
    notes text,
    ingredient_id varchar(255),
    nutrition_source varchar(255),
    primary key (recipe_id, group_position, position),
    foreign key (recipe_id, group_position)
        references public.recipe_ingredient_group(recipe_id, position)
        on delete cascade
);

create table public.recipe_ingredient_substitution (
    recipe_id varchar(255) not null,
    group_position integer not null,
    ingredient_position integer not null,
    position integer not null,
    substitution text,
    primary key (recipe_id, group_position, ingredient_position, position),
    foreign key (recipe_id, group_position, ingredient_position)
        references public.recipe_ingredient(recipe_id, group_position, position)
        on delete cascade
);

create table public.recipe_instruction (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    step_number integer,
    phase varchar(255),
    text text,
    structured_action varchar(255),
    structured_duration varchar(255),
    temperature_celsius integer,
    temperature_fahrenheit integer,
    doneness_visual text,
    doneness_tactile text,
    primary key (recipe_id, position)
);

create table public.recipe_instruction_tip (
    recipe_id varchar(255) not null,
    instruction_position integer not null,
    position integer not null,
    tip text,
    primary key (recipe_id, instruction_position, position),
    foreign key (recipe_id, instruction_position)
        references public.recipe_instruction(recipe_id, position)
        on delete cascade
);

create table public.recipe_troubleshooting (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    symptom text,
    likely_cause text,
    prevention text,
    fix text,
    primary key (recipe_id, position)
);

create table public.recipe_nutrition_source (
    recipe_id varchar(255) not null references public.recipe(recipe_id) on delete cascade,
    position integer not null,
    source text,
    primary key (recipe_id, position)
);
