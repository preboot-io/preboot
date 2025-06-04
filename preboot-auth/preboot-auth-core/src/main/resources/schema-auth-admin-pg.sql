-- Create super-admin user if it doesn't exist
INSERT INTO user_accounts (
    uuid,
    username,
    email,
    language,
    timezone,
    active,
    version,
    reset_token_version
)
SELECT
    gen_random_uuid(),
    'super-admin',
    'super-admin@system.local',
    'en',
    'UTC',
    true,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM user_accounts WHERE email = 'super-admin@system.local'
);

-- Create role for the super-admin if it doesn't exist
INSERT INTO user_account_roles (user_accounts, name, tenant_id)
SELECT id, 'super-admin', '00000000-0000-0000-0000-000000000000'
FROM user_accounts
WHERE email = 'super-admin@system.local'
  AND NOT EXISTS (
    SELECT 1 FROM user_account_roles WHERE name = 'super-admin'
);

-- Add role permissions if they don't exist
INSERT INTO user_account_role_permissions (role, name)
SELECT p.role, p.permission_name
FROM (
         VALUES
             ('super-admin', 'ADMIN_ACCESS'),
             ('super-admin', 'USER_MANAGEMENT'),
             ('super-admin', 'SYSTEM_CONFIGURATION')
     ) AS p(role, permission_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM user_account_role_permissions
    WHERE role = p.role AND name = p.permission_name
);

-- Add credential if it doesn't exist (changeme)
INSERT INTO user_account_credentials (user_accounts, type, attribute)
SELECT ua.id, 'PASSWORD', '{bcrypt}$2a$12$njKaLRkUyfiNy1B1JkRadeMEtKBmjSXbjMlBA4vNoBAIVEgB6/JlW'
FROM user_accounts ua
WHERE ua.email = 'super-admin@system.local'
  AND NOT EXISTS (
    SELECT 1
    FROM user_account_credentials
    WHERE user_accounts = ua.id
      AND type = 'PASSWORD'
);


CREATE OR REPLACE VIEW user_accounts_info_view AS
WITH combined_permissions AS (
    -- Direct user permissions
    SELECT
        ua.id as user_id,
        uap.name as permission_name,
        uap.tenant_id as tenant_id
    FROM user_accounts ua
             LEFT JOIN user_account_permissions uap ON uap.user_accounts = ua.id
    WHERE uap.name IS NOT NULL

    UNION DISTINCT

    -- Role-based permissions
    SELECT
        ua.id as user_id,
        urp.name as permission_name,
        uar.tenant_id as tenant_id
    FROM user_accounts ua
             LEFT JOIN user_account_roles uar ON uar.user_accounts = ua.id
             LEFT JOIN user_account_role_permissions urp ON urp.role = uar.name
    WHERE urp.name IS NOT NULL
),
     tenant_users AS (
         -- Get distinct user-tenant combinations from both roles and permissions
         SELECT DISTINCT ua.id as user_id, uar.tenant_id
         FROM user_accounts ua
                  LEFT JOIN user_account_roles uar ON uar.user_accounts = ua.id
         WHERE uar.tenant_id IS NOT NULL

         UNION DISTINCT

         SELECT DISTINCT ua.id as user_id, uap.tenant_id
         FROM user_accounts ua
                  LEFT JOIN user_account_permissions uap ON uap.user_accounts = ua.id
         WHERE uap.tenant_id IS NOT NULL
     )
SELECT
    ua.id,
    ua.uuid,
    ua.username,
    ua.email,
    ua.active,
    tu.tenant_id,
    t.name as tenant_name,
    STRING_AGG(DISTINCT uar.name, ',') as roles,
    STRING_AGG(DISTINCT cp.permission_name, ',') as permissions
FROM user_accounts ua
         JOIN tenant_users tu ON tu.user_id = ua.id
         LEFT JOIN user_account_roles uar ON uar.user_accounts = ua.id AND uar.tenant_id = tu.tenant_id
         LEFT JOIN combined_permissions cp ON cp.user_id = ua.id AND cp.tenant_id = tu.tenant_id
         LEFT JOIN tenants t ON t.uuid = tu.tenant_id
GROUP BY ua.id, ua.uuid, ua.username, ua.email, ua.active, tu.tenant_id, t.name;
