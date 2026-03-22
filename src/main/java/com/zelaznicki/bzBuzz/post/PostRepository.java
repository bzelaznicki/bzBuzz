package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.common.Status;
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
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
 * Finds a post by its URL-friendly slug.
 *
 * @param slug the unique URL identifier for the post
 * @return an Optional containing the matching Post, or Optional.empty() if no match is found
 */
Optional<Post> findBySlug(String slug);

    /**
 * Finds posts within the specified board that have the specified status, ordered by creation time descending.
 *
 * @param board  the board to filter posts by
 * @param status the status to filter posts by
 * @return a list of posts matching the board and status, ordered by `createdAt` descending
 */
List<Post> findAllByBoardAndStatusOrderByCreatedAtDesc(Board board, Status status);
    /**
 * Retrieves posts from the given board with the specified status, ordered by `updatedAt` descending.
 *
 * @param board  the board whose posts should be retrieved
 * @param status the status that retrieved posts must have
 * @return       a list of matching Post entities ordered by most recently updated first
 */
List<Post> findAllByBoardAndStatusOrderByUpdatedAtDesc(Board board, Status status);
    /**
 * Finds posts within the specified board that have the given status, ordered by vote score descending.
 *
 * @param board  the board to filter posts by
 * @param status the status to filter posts by
 * @return       a list of Post entities matching the board and status, ordered by vote score (highest first)
 */
List<Post> findAllByBoardAndStatusOrderByVoteScoreDesc(Board board, Status status);
    /**
 * Retrieve posts created by the given user with the specified status, sorted by creation time descending.
 *
 * @param user   the creator whose posts to retrieve
 * @param status the status to filter posts by
 * @return       a list of posts created by the user with the given status, ordered by newest first (createdAt descending)
 */
List<Post> findAllByCreatorAndStatusOrderByCreatedAtDesc(User user, Status status);
    /**
 * Finds posts created by the given user with the specified status, ordered by most recently updated first.
 *
 * @param user   the creator whose posts to retrieve
 * @param status the status that matching posts must have
 * @return       the list of matching posts ordered by `updatedAt` descending
 */
List<Post> findAllByCreatorAndStatusOrderByUpdatedAtDesc(User user, Status status);
    /**
 * Retrieve posts created by the given user with the specified status, ordered by vote score descending.
 *
 * @param user   the creator whose posts to return
 * @param status the status to filter posts by
 * @return a list of posts matching the creator and status, ordered by `voteScore` from highest to lowest
 */
List<Post> findAllByCreatorAndStatusOrderByVoteScoreDesc(User user, Status status);

    /**
     * Fetches a post by its id while acquiring a pessimistic write lock on the selected row.
     *
     * @param id the UUID of the post to fetch
     * @return an Optional containing the matching Post locked for writing, or empty if no post exists with the given id
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Increase a post's vote score by one.
     *
     * @param id the UUID of the post to increment the vote score for
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore + 1 WHERE p.id = :id")
    void incrementVoteScore(@Param("id") UUID id);

    /**
     * Decrements the vote score of the post with the given id by one.
     *
     * @param id the UUID of the Post whose vote score will be decreased by one
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore - 1 WHERE p.id = :id")
    void decrementVoteScore(@Param("id") UUID id);

    /**
     * Adjusts the vote score of the post identified by the given id by the specified delta.
     *
     * @param id    the UUID of the post to update
     * @param delta the amount to add to the post's vote score; positive values increase the score,
     *              negative values decrease it
     */
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore + :delta WHERE p.id = :id")
    void adjustVoteScore(@Param("id") UUID id, @Param("delta") int delta);


    Optional<Post> findBySlugAndStatus(String slug,  Status status);
}
