-- ============================================================================
-- SHARAMIGO MIGRATION V3: profile page and listing photos
-- Run once on your existing database, after migration_v2.sql.
-- ============================================================================

USE sharamigo_db;

-- Profile page: short bio and profile picture
ALTER TABLE users
    ADD COLUMN bio VARCHAR(300) NULL AFTER student_number,
    ADD COLUMN avatar_url VARCHAR(255) NULL AFTER bio;

-- Listing photos: up to 4 per item. items.photo_url keeps a copy of the first one
-- so the campus feed works without changes.
CREATE TABLE IF NOT EXISTS item_photos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    photo_path VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    INDEX idx_item_photos_item (item_id, sort_order)
) ENGINE=InnoDB;
