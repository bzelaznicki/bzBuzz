ALTER TABLE comments
    DROP CONSTRAINT chk_status;

ALTER TABLE comments
    ADD CONSTRAINT chk_status CHECK (status IN ('ENABLED', 'DISABLED', 'REMOVED'));

ALTER TABLE posts
DROP CONSTRAINT chk_status;

ALTER TABLE posts
    ADD CONSTRAINT chk_status CHECK (status IN ('ENABLED', 'DISABLED', 'REMOVED'));