create table app_schema_version_marker (
    id          integer primary key,
    description text not null
);

insert into app_schema_version_marker (id, description)
values (1, 'backend foundation initialized');
