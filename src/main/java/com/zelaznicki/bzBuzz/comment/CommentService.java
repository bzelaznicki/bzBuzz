package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.common.PostSort;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.VoteResponse;
import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;

    private static final int UPVOTE = 1;
    private static final int DOWNVOTE = -1;

    public Comment addComment(User user, Post post, Comment parent, String body) {

            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("Comment body cannot be empty");
            }

            Comment comment = Comment.builder()
                    .user(user)
                    .post(post)
                    .parent(parent)
                    .voteScore(0)
                    .status(Status.ENABLED)
                    .body(body)
                    .build();

            return commentRepository.save(comment);
    }

    public List<Comment> findParentCommentsByPost(Post post, PostSort postSort) {
        return switch (postSort) {
            case NEW -> commentRepository.findAllByPostAndParentIsNullOrderByCreatedAtDesc(post);
            case TOP -> commentRepository.findAllByPostAndParentIsNullOrderByVoteScoreDesc(post);
            default -> throw new IllegalArgumentException("Comments can only be sorted by NEW or TOP");
        };
    }

    public List<Comment> findAllChildCommentsByParent(Comment parent) {
        return commentRepository.findAllByParentOrderByCreatedAtAsc(parent);
    }

    @Transactional
    public VoteResponse vote(Comment comment, User user, int voteType) {

        if (voteType != UPVOTE && voteType != DOWNVOTE) {
            throw new IllegalArgumentException("Invalid vote");
        }

        Comment lockedComment = commentRepository.findByIdForUpdate(comment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));


        int delta = 0;
        String action;
        Optional<CommentVote> existing = commentVoteRepository.findByCommentAndUser(lockedComment, user);
        if (existing.isPresent()) {
            CommentVote currentVote = existing.get();

            if (currentVote.getVoteType() == voteType) {
                action = "unvoted";
                commentVoteRepository.delete(currentVote);
                delta = -voteType;
            } else {
                action = voteType == UPVOTE ? "upvoted" : "downvoted";
                currentVote.setVoteType(voteType);
                commentVoteRepository.save(currentVote);
                delta = voteType * 2;
            }
        } else {
            action = voteType == UPVOTE ? "upvoted" : "downvoted";
            CommentVote commentVote = CommentVote.builder()
                    .comment(lockedComment)
                    .user(user)
                    .voteType(voteType)
                    .build();
            commentVoteRepository.saveAndFlush(commentVote);
            delta = voteType;
        }

        int expectedScore = lockedComment.getVoteScore() + delta;
        commentRepository.adjustVoteScore(lockedComment.getId(), delta);
        return new VoteResponse(expectedScore, action);
    }

    public Comment updateComment(User user, Comment comment, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Comment body cannot be empty");
        }

        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to perform this action");
        }

        comment.setBody(body);
        return commentRepository.save(comment);
    }
    public void deleteComment(User user, Comment comment) {
        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not permitted to perform this action");
        }
        comment.setStatus(Status.DISABLED);
        comment.setBody("[deleted]");
        commentRepository.save(comment);
    }

    public Map<UUID, Integer> findVotesByPostAndUser(Post post, User user) {
        return commentVoteRepository.findByUserAndPost(user, post)
                .stream()
                .collect(Collectors.toMap(
                        cv -> cv.getComment().getId(),
                        CommentVote::getVoteType
                ));
    }

    Comment getComment(UUID commentId) {

        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    public List<Comment> findAllRepliesByPost(Post post) {
        return commentRepository.findAllByPostAndParentIsNotNullOrderByCreatedAtAsc(post);
    }
}
