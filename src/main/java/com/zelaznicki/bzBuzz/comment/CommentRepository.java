package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    long countByPost(Post post);

    List<Comment> findAllByPostAndParentIsNullOrderByVoteScoreDesc(Post post);
    List<Comment> findAllByPostAndParentIsNullOrderByCreatedAtDesc(Post post);
    List<Comment> findAllByParentOrderByCreatedAtAsc(Comment parent);
    /**
 * Fetches all comments authored by the given user.
 *
 * @param user the author whose comments should be retrieved
 * @return a list of comments authored by the specified user
 */
List<Comment> findAllByUser(User user);
    /**
 * Fetches child comments for the specified post, ordered by creation time ascending.
 *
 * @param post the post whose non-top-level (has a parent) comments should be returned
 * @return the list of comments for the given post where `parent` is not null, sorted by `createdAt` ascending
 */
List<Comment> findAllByPostAndParentIsNotNullOrderByCreatedAtAsc(Post post);

    /**
     * Count comments with the given status for each post in the provided collection.
     *
     * @param posts  the posts to include in the grouped count
     * @param status the comment status to filter by
     * @return a list of result rows where each Object[] contains two elements: index 0 is the post id, index 1 is the comment count (Long)
     */
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post IN :posts AND c.status = :status GROUP BY c.post.id")
    List<Object[]> countByPostsAndStatus(@Param("posts") List<Post> posts, @Param("status") Status status);

    /**
     * Fetches the Comment with the given id while acquiring a pessimistic write lock on the selected row.
     *
     * @param id the UUID of the Comment to fetch
     * @return an Optional containing the Comment if found, empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c from Comment c WHERE c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.voteScore = c.voteScore + :delta WHERE c.id = :id")
    void adjustVoteScore(@Param("id") UUID id, @Param("delta") int delta);


}
