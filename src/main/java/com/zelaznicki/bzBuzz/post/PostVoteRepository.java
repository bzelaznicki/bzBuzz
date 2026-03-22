package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
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
public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {

    /**
 * Finds the vote for a given post by a specific user.
 *
 * @param post the post to find the vote for
 * @param user the user who cast the vote
 * @return an Optional containing the matching PostVote if present, otherwise an empty Optional
 */
Optional<PostVote> findByPostAndUser(Post post, User user);
    /**
 * Check whether a vote exists for a specific post by a specific user.
 *
 * @param post the post to check for an existing vote
 * @param user the user who may have cast the vote
 * @return `true` if a `PostVote` exists for the given `post` and `user`, `false` otherwise
 */
boolean existsByPostAndUser(Post post, User user);

    /**
     * Delete any PostVote entities that are associated with the specified post and user.
     *
     * @param post the post whose votes should be removed
     * @param user the user whose vote on the post should be removed
     */
    @Modifying
    @Transactional
    void deleteByPostAndUser(Post post, User user);


    /**
     * Load a PostVote by its id and acquire a pessimistic write lock on the retrieved entity.
     *
     * @param id the UUID primary key of the PostVote to load
     * @return an Optional containing the PostVote with the given id if present, empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM PostVote pv WHERE pv.id = :id")
    Optional<PostVote> findByIdForUpdate(@Param("id") UUID id);

    /**
 * Retrieve all votes made by the specified user for posts contained in the provided list.
 *
 * @param user  the user whose votes to retrieve
 * @param posts the list of posts to filter votes by
 * @return a list of matching PostVote entities; empty if none exist
 */
List<PostVote> findByUserAndPostIn(User user, List<Post> posts);

    @Query("SELECT pv FROM PostVote pv WHERE pv.user = :user AND pv.post.board = :board AND pv.post.status = 'ENABLED'")
    List<PostVote> findByUserAndBoard(@Param("user") User user, @Param("board") Board board);
}
