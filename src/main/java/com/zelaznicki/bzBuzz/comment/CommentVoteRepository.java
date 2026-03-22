package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<CommentVote> findByUserAndCommentIn(User user, List<Comment> comments);

}
