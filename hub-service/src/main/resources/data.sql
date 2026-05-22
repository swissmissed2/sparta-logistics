DROP INDEX IF EXISTS uk_hub_stock_hub_product;

CREATE UNIQUE INDEX IF NOT EXISTS uk_hub_stock_hub_product_active
    ON schema_hub.p_hub_stock (hub_id, product_id)
    WHERE deleted_at IS NULL;