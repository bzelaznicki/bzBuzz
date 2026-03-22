package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentVote {
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id",  nullable = false)
    private Comment comment;

    @Column(name = "vote_type",  nullable = false)
    private int voteType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the `createdAt` timestamp to the current local date-time before the entity is persisted.
     *
     * Called by the JPA lifecycle on initial persist.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
