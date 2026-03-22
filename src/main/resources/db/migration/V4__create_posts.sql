CREATE TABLE posts (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    board_id UUID NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    text TEXT,
    post_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    url VARCHAR(255),
    vote_score INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_post_type CHECK (post_type IN ('TEXT', 'URL')),
    CONSTRAINT chk_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_text_post CHECK (post_type != 'TEXT' OR text IS NOT NULL),
    CONSTRAINT chk_url_post CHECK (post_type != 'URL' OR url IS NOT NULL),
    CONSTRAINT chk_not_both CHECK (NOT (text IS NOT NULL AND url IS NOT NULL))
);

CREATE INDEX idx_posts_board_id ON posts(board_id);
CREATE INDEX idx_posts_board_created ON posts(board_id, created_at DESC);