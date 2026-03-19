CREATE TABLE boards_users (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    board_id UUID NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, board_id),
    CONSTRAINT chk_role CHECK (role IN ('MEMBER', 'MODERATOR'))
);

CREATE INDEX idx_boards_users_board_role ON boards_users (board_id, role);