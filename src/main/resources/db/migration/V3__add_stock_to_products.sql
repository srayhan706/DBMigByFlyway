-- NOT NULL on a table that already has rows needs a value for those rows.
-- The DEFAULT fills them in (and any future insert that omits stock).
ALTER TABLE products
    ADD COLUMN stock INTEGER NOT NULL DEFAULT 0;
