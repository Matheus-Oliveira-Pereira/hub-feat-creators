-- ShedLock 5.x expects lock_until instead of expires_at + lock_at
ALTER TABLE shedlock RENAME COLUMN expires_at TO lock_until;
ALTER TABLE shedlock DROP COLUMN IF EXISTS lock_at;
