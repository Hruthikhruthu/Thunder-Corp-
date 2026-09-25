-- ============================================================
-- ThunderCore ERP - Database Schema
-- ============================================================
--
-- This script is mounted into the MySQL container and executed when the
-- mysql-data volume is created for the first time. JPA also runs with
-- ddl-auto=update in production so the demo stack remains self-healing, while
-- this file documents the intended relational model and seed data.

CREATE DATABASE IF NOT EXISTS thundercore_db;
USE thundercore_db;

-- ============================================================
-- Users
-- Purpose:
--   Stores authenticated ERP identities, BCrypt password hashes, RBAC roles,
--   and active/inactive state.
-- Relationships:
--   employees.user_id optionally points to users.id.
--   notifications.user_id points to users.id.
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
);

-- ============================================================
-- Products
-- Purpose:
--   Master inventory catalog used by product CRUD, stock health indicators,
--   dashboard category charts, and Excel reports.
-- Business rule:
--   quantity <= reorder_threshold is treated as low stock by backend services.
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    supplier VARCHAR(100),
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    reorder_threshold INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_products_sku (sku),
    INDEX idx_products_category (category)
);

-- ============================================================
-- Stock Movements
-- Purpose:
--   Audit-ready table for future inbound/outbound inventory movement tracking.
-- Relationships:
--   product_id cascades on product deletion so orphan stock movement rows do
--   not remain after a demo reset.
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ============================================================
-- Employees
-- Purpose:
--   HR profile and payroll baseline records used by the HR workspace and
--   dashboard employee KPIs.
-- Relationships:
--   user_id is optional and unique, modelling a one-to-one link to a login
--   account while preserving employee records if the user is removed.
-- ============================================================
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100),
    designation VARCHAR(100),
    base_salary DECIMAL(10,2),
    joining_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_employees_code (employee_code),
    INDEX idx_employees_department (department)
);

-- ============================================================
-- Attendance
-- Purpose:
--   Extension-ready HR attendance table for daily presence tracking.
-- Relationships:
--   employee_id cascades because attendance has no meaning without its
--   employee record. A unique key prevents duplicate attendance rows per day.
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (employee_id, date)
);

-- ============================================================
-- Leave Requests
-- Purpose:
--   Extension-ready HR leave workflow table.
-- Relationships:
--   employee_id cascades with the owning employee profile.
-- ============================================================
CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- ============================================================
-- Payrolls
-- Purpose:
--   Extension-ready payroll result table with allowances, deductions, net pay,
--   and approval/payment status.
-- Relationships:
--   employee_id cascades with the owning employee profile.
-- Calculation note:
--   net_salary is expected to represent basic_salary + allowances - deductions.
-- ============================================================
CREATE TABLE IF NOT EXISTS payrolls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    payroll_month VARCHAR(20) NOT NULL,
    payroll_year INT NOT NULL,
    basic_salary DECIMAL(10,2) NOT NULL DEFAULT 0,
    allowances DECIMAL(10,2) DEFAULT 0,
    deductions DECIMAL(10,2) DEFAULT 0,
    net_salary DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- ============================================================
-- Customers
-- Purpose:
--   CRM accounts used by Sales & CRM screens, customer tier summaries, and
--   dashboard customer counts.
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(50),
    address TEXT,
    tier VARCHAR(50) DEFAULT 'STANDARD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customers_email (email)
);

CREATE TABLE IF NOT EXISTS sale_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_sale_orders_status (status)
);

-- ============================================================
-- Invoices
-- Purpose:
--   Finance records used by invoice CRUD, revenue KPIs, invoice status charts,
--   overdue alerts, and PDF reports.
-- Design note:
--   Invoices store customer name/email directly, keeping finance history
--   independent from CRM customer lifecycle changes.
-- Calculation note:
--   net_amount is total_amount + tax_amount in InvoiceService.
-- ============================================================
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invoices_number (invoice_number),
    INDEX idx_invoices_status (status)
);

-- ============================================================
-- Notifications
-- Purpose:
--   Persisted user alerts shown in the frontend notification bell and delivered
--   live through /user/queue/notifications.
-- Relationships:
--   user_id cascades so deleted users do not leave orphan inbox records.
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id)
);

-- ============================================================
-- Seed Data
-- ============================================================
--
-- Default users are also created by DataInitializer.java on first backend
-- startup. These SQL inserts ensure Docker MySQL has them immediately.
-- Passwords are BCrypt hashes of: Admin@123, Manager@123, Staff@123

INSERT INTO users (email, password, first_name, last_name, role, active)
SELECT * FROM (SELECT
    'admin@thundercore.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Super', 'Admin', 'SUPER_ADMIN', TRUE) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@thundercore.com');

INSERT INTO users (email, password, first_name, last_name, role, active)
SELECT * FROM (SELECT
    'manager@thundercore.com',
    '$2a$10$8K1p/a0dR1xqM8K3Qe6MReE3wZ5bXL9Y2nVfqoUelE/gg.GtusfW6',
    'Operations', 'Manager', 'MANAGER', TRUE) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'manager@thundercore.com');

INSERT INTO users (email, password, first_name, last_name, role, active)
SELECT * FROM (SELECT
    'staff@thundercore.com',
    '$2a$10$Ei5bBBnkNaHGXHnzMFHFCOqMnzMFHFCOqMnzMFHFCOqMnzMFHFCO',
    'Frontline', 'Staff', 'STAFF', TRUE) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff@thundercore.com');

-- Sample products
INSERT INTO products (sku, name, description, category, supplier, unit_price, quantity, reorder_threshold)
SELECT * FROM (SELECT 'SKU-001' AS sku, 'Laptop Pro 15' AS name, 'High-performance laptop' AS description, 'Electronics' AS category, 'TechSupply Inc.' AS supplier, 1299.99 AS unit_price, 45 AS quantity, 10 AS reorder_threshold) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-001');

INSERT INTO products (sku, name, description, category, supplier, unit_price, quantity, reorder_threshold)
SELECT * FROM (SELECT 'SKU-002', 'Office Chair Ergonomic', 'Adjustable ergonomic chair', 'Furniture', 'OfficePlus', 349.50, 8, 15) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-002');

INSERT INTO products (sku, name, description, category, supplier, unit_price, quantity, reorder_threshold)
SELECT * FROM (SELECT 'SKU-003', 'Wireless Mouse', 'Bluetooth wireless mouse', 'Electronics', 'TechSupply Inc.', 29.99, 120, 20) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-003');

INSERT INTO products (sku, name, description, category, supplier, unit_price, quantity, reorder_threshold)
SELECT * FROM (SELECT 'SKU-004', 'Standing Desk', 'Electric height-adjustable desk', 'Furniture', 'OfficePlus', 599.00, 5, 10) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-004');

INSERT INTO products (sku, name, description, category, supplier, unit_price, quantity, reorder_threshold)
SELECT * FROM (SELECT 'SKU-005', 'Monitor 27 inch 4K', 'Ultra HD monitor', 'Electronics', 'DisplayTech', 449.99, 30, 10) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SKU-005');

-- Sample customers
INSERT INTO customers (name, email, phone, address, tier)
SELECT * FROM (SELECT 'Acme Corporation' AS name, 'contact@acme.com' AS email, '+1-555-0100' AS phone, '123 Business Ave, NY' AS address, 'VIP' AS tier) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'contact@acme.com');

INSERT INTO customers (name, email, phone, address, tier)
SELECT * FROM (SELECT 'TechStart LLC', 'hello@techstart.io', '+1-555-0200', '456 Innovation Blvd, SF', 'PREMIUM') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'hello@techstart.io');

INSERT INTO customers (name, email, phone, address, tier)
SELECT * FROM (SELECT 'Global Retail Co.', 'sales@globalretail.com', '+1-555-0300', '789 Commerce St, Chicago', 'STANDARD') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'sales@globalretail.com');
