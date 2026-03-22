package com.zelaznicki.bzBuzz.comment;

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
    List<Comment> findAllByUser(User user);
    List<Comment> findAllByPostAndParentIsNotNullOrderByCreatedAtAsc(Post post);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c from Comment c WHERE c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Comment c SET c.voteScore = c.voteScore + :delta WHERE c.id = :id")
    void adjustVoteScore(@Param("id") UUID id, @Param("delta") int delta);


}
