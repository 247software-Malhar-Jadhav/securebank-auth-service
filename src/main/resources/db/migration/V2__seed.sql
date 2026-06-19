-- V2__seed.sql
-- Seed two demo identities so the platform is usable out of the box.
--
-- Both passwords are the literal string "Password123!".
-- The value below is a real BCrypt hash of "Password123!" (cost factor 10),
-- computed with Spring Security's BCryptPasswordEncoder and verified (matches=true).
-- We hardcode it (rather than hashing at runtime) so the migration is deterministic
-- and reproducible across environments. Rotate these in any real deployment.
--
--   admin  / Password123!  -> ADMIN
--   jsmith / Password123!  -> CUSTOMER

INSERT INTO users (username, email, password_hash, role, enabled, preferred_locale)
VALUES
  ('admin',  'admin@securebank.local',
   '$2a$10$JdbuOSVxPAtY4dAXH2dyzeUg13PV.zKC0ChioMO3YgELyB7WwdHpS',
   'ADMIN', TRUE, 'en'),
  ('jsmith', 'jsmith@securebank.local',
   '$2a$10$JdbuOSVxPAtY4dAXH2dyzeUg13PV.zKC0ChioMO3YgELyB7WwdHpS',
   'CUSTOMER', TRUE, 'en');
