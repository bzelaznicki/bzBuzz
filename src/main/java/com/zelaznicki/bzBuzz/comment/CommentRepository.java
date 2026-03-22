package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c from Comment c WHERE c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") UUID id);


}
