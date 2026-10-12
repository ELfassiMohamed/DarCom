-- seed_admin.sql — reference only, NOT auto-executed. Run manually with psql after deploy.
--
-- 1. Generate a BCrypt (cost 12) hash for the admin password first, e.g. with a tiny Java main:
--      System.out.println(new com.backend.security.PasswordService().hash("pick-a-strong-password"));
--    Never commit a real password or hash here.
-- 2. Replace <BCRYPT_COST_12_HASH> below and run:
--      psql "$DB_URL" -f seed_admin.sql

INSERT INTO users (id, email, password_hash, full_name, phone, role, verified, blocked, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'admin@darcom.local',
    '<BCRYPT_COST_12_HASH>',
    'DarCom Admin',
    NULL,
    'ADMIN',
    TRUE,
    FALSE,
    NOW()
)
ON CONFLICT (email) DO NOTHING;
