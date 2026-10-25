-- Run this whole script in MySQL Workbench (lightning-bolt button).
-- It is safe to run more than once.
--
-- If you already ran the OLD schema (users table without a role column), run this ONE line first:
--   ALTER TABLE food_delivery.users ADD COLUMN role ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER';

CREATE DATABASE IF NOT EXISTS food_delivery;
USE food_delivery;

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    salt          CHAR(32)     NOT NULL,
    full_name     VARCHAR(80)  NOT NULL,
    phone         CHAR(10)     NOT NULL,
    address       VARCHAR(200) NOT NULL,
    role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_items (
    item_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(60)  NOT NULL,
    category  VARCHAR(30)  NOT NULL,
    price     DECIMAL(8,2) NOT NULL CHECK (price > 0),
    available BOOLEAN      NOT NULL DEFAULT TRUE      -- "removed" items are hidden, not deleted
);

CREATE TABLE IF NOT EXISTS orders (
    order_id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT NOT NULL,
    total            DECIMAL(10,2) NOT NULL,
    status           ENUM('PLACED','PREPARING','OUT_FOR_DELIVERY','DELIVERED') NOT NULL DEFAULT 'PLACED',
    delivery_address VARCHAR(200) NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT NOT NULL,
    item_id       INT NOT NULL,
    quantity      INT NOT NULL CHECK (quantity > 0),
    unit_price    DECIMAL(8,2) NOT NULL,              -- price AT THE TIME of the order
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (item_id)  REFERENCES menu_items(item_id)
);

-- Sample menu (only inserted when the menu is empty)
INSERT INTO menu_items (name, category, price)
SELECT * FROM (
    SELECT 'Margherita Pizza' AS n, 'Pizza'    AS c, 249.00 AS p UNION ALL
    SELECT 'Paneer Tikka Pizza',    'Pizza',    299.00 UNION ALL
    SELECT 'Veg Burger',            'Burger',   129.00 UNION ALL
    SELECT 'Chicken Burger',        'Burger',   159.00 UNION ALL
    SELECT 'Paneer Butter Masala',  'Main',     220.00 UNION ALL
    SELECT 'Veg Biryani',           'Main',     180.00 UNION ALL
    SELECT 'Chicken Biryani',       'Main',     240.00 UNION ALL
    SELECT 'French Fries',          'Snacks',    90.00 UNION ALL
    SELECT 'Masala Dosa',           'Snacks',   110.00 UNION ALL
    SELECT 'Cold Coffee',           'Drinks',    99.00 UNION ALL
    SELECT 'Mango Lassi',           'Drinks',    80.00 UNION ALL
    SELECT 'Gulab Jamun (2 pcs)',   'Dessert',   70.00
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM menu_items);

-- To make someone an admin, register them in the app first, then run:
--   UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
