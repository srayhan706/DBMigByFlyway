-- Fails if the table already contains two products with the same name:
-- a constraint migration depends on the data, not only on the structure.
ALTER TABLE products
    ADD CONSTRAINT uk_products_name UNIQUE (name);
