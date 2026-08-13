-- Priority 1 (Maintenance + Inventory integration): lets a rapport part line
-- be traced back to the PartReservation it was drawn from, and lets a
-- reservation record how much of what was reserved was actually consumed
-- (may be less than quantity_reserved - the unused remainder needs no
-- separate release, it stops counting as "reserved" the moment status
-- leaves RESERVED). Both nullable: existing free-text rapport part rows and
-- already-consumed/released reservations are unaffected.

ALTER TABLE rapport_parts ADD COLUMN IF NOT EXISTS part_id BIGINT;

ALTER TABLE part_reservations ADD COLUMN IF NOT EXISTS quantity_consumed INT;
