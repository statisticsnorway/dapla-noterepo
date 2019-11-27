create table note (
    id uuid not null primary key,
    name varchar(255),
    description text,
    namespace_id uuid
)