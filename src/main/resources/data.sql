-- Runs at startup for embedded databases (spring.sql.init.mode=embedded is the default),
-- after Hibernate creates the schema (spring.jpa.defer-datasource-initialization=true).
-- Runs in EVERY profile. For profile-specific seed data, see config/DataSeeder (CommandLineRunner).
INSERT INTO users (username, email, role, active) VALUES
    ('admin',   'admin@example.com', 'ADMIN', TRUE),
    ('qa-user', 'qa@example.com',    'QA',    TRUE),
    ('alice',   'alice@example.com', 'USER',  TRUE),
    ('bob',     'bob@example.com',   'USER',  FALSE);
