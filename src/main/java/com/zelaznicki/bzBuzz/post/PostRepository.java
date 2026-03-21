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

    Optional<Post> findBySlug(String slug);

    List<Post> findAllByBoardAndStatusOrderByCreatedAtDesc(Board board, Status status);
    List<Post> findAllByBoardAndStatusOrderByUpdatedAtDesc(Board board, Status status);
    List<Post> findAllByBoardAndStatusOrderByVoteScoreDesc(Board board, Status status);
    List<Post> findAllByCreatorAndStatusOrderByCreatedAtDesc(User user, Status status);
    List<Post> findAllByCreatorAndStatusOrderByUpdatedAtDesc(User user, Status status);
    List<Post> findAllByCreatorAndStatusOrderByVoteScoreDesc(User user, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore + 1 WHERE p.id = :id")
    void incrementVoteScore(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore - 1 WHERE p.id = :id")
    void decrementVoteScore(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.voteScore = p.voteScore + :delta WHERE p.id = :id")
    void adjustVoteScore(@Param("id") UUID id, @Param("delta") int delta);
}
