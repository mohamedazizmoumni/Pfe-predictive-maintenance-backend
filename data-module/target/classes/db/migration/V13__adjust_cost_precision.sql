-- Align monetary columns with BigDecimal precision requirements
ALTER TABLE IF EXISTS parts
    ALTER COLUMN cost TYPE NUMERIC(19,4)
    USING cost::NUMERIC(19,4);

ALTER TABLE IF EXISTS stock_orders
    ALTER COLUMN cost TYPE NUMERIC(19,4)
    USING cost::NUMERIC(19,4);
