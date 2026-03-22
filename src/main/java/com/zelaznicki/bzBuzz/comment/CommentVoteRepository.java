package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, UUID> {

    Optional<CommentVote> findByCommentAndUser(Comment comment, User user);

    @Modifying
    @Transactional
    void deleteByCommentAndUser(Comment comment, User user);

    @Query("SELECT cv FROM CommentVote cv WHERE cv.user = :user AND cv.comment.post = :post")
    List<CommentVote> findByUserAndPost(@Param("user") User user, @Param("post") Post post);
}
