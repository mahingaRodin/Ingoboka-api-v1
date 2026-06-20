ALTER TABLE insurance_products
    ADD COLUMN IF NOT EXISTS hero_image_key VARCHAR(512);

CREATE TABLE IF NOT EXISTS product_documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES insurance_products(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    object_key  VARCHAR(512) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_documents_product_id ON product_documents(product_id);
