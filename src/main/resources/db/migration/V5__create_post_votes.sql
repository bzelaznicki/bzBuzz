CREATE TABLE post_votes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    vote_type INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, post_id),
    CONSTRAINT chk_vote_type CHECK (vote_type IN (1, -1))
);

CREATE INDEX idx_post_votes_post_id ON post_votes(post_id);