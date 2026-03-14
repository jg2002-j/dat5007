create table public.meals
(
    id          bigserial primary key,
    tdate       date         not null,
    slot        varchar(255) not null,
    recipe_uuid varchar(255) not null references public.recipe (recipe_id),
);