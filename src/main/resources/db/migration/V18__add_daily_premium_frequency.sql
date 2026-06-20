ALTER TABLE product_plans DROP CONSTRAINT IF EXISTS product_plans_frequency_check;
ALTER TABLE product_plans ADD CONSTRAINT product_plans_frequency_check CHECK (
    premium_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUAL')
);

CREATE TABLE IF NOT EXISTS product_faq (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES insurance_products(id) ON DELETE CASCADE,
    question    VARCHAR(512) NOT NULL,
    answer      TEXT NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_faq_product_id ON product_faq(product_id);
