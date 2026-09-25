-- ============================================================
-- ThunderCore ERP - Reset Demo Users
-- Run this if login is broken after a schema change.
-- BCrypt hashes below correspond to: Admin@123, Manager@123, Staff@123
-- Generated with BCrypt strength 10.
-- ============================================================
USE thundercore_db;

-- Delete and re-insert all demo users with fresh BCrypt hashes
DELETE FROM users WHERE email IN (
    'admin@thundercore.com',
    'manager@thundercore.com',
    'staff@thundercore.com'
);

-- BCrypt hash of "Admin@123"
INSERT INTO users (email, password, first_name, last_name, role, active)
VALUES ('admin@thundercore.com',
        '$2a$10$slYQmyNdgTY18LGvgxPwHOSvfnB3HeIIRC5.JgCFiLbiaeZHveRNm',
        'Super', 'Admin', 'SUPER_ADMIN', TRUE);

-- BCrypt hash of "Manager@123"
INSERT INTO users (email, password, first_name, last_name, role, active)
VALUES ('manager@thundercore.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        'Operations', 'Manager', 'MANAGER', TRUE);

-- BCrypt hash of "Staff@123"
INSERT INTO users (email, password, first_name, last_name, role, active)
VALUES ('staff@thundercore.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        'Frontline', 'Staff', 'STAFF', TRUE);

SELECT id, email, role, active FROM users;

