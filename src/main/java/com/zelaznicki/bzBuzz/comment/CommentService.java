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

    private static void requireAuthenticated(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    public Comment addComment(User user, Post post, Comment parent, String body) {
            requireAuthenticated(user);
            if (post.getStatus() == Status.DISABLED) {
                throw new IllegalArgumentException("Cannot comment on a deleted post");
            }
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("Comment body cannot be empty");
            }

            if (parent != null && parent.getStatus()== Status.DISABLED) {
                throw new IllegalArgumentException("Cannot reply to a deleted comment");
            }

            if (parent != null && (parent.getPost() == null || !parent.getPost().getId().equals(post.getId())))  {
                throw new IllegalArgumentException("Reply parent must belong to the same post");
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
        requireAuthenticated(user);
        if (voteType != UPVOTE && voteType != DOWNVOTE) {
            throw new IllegalArgumentException("Invalid vote");
        }

        Comment lockedComment = commentRepository.findByIdForUpdate(comment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));


        if (lockedComment.getStatus() == Status.DISABLED) {
            throw new IllegalArgumentException("Comment is deleted");
        }

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

    @Transactional
    public Comment updateComment(User user, Comment comment, String body) {
        requireAuthenticated(user);
        Comment managedComment = commentRepository.findByIdForUpdate(comment.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (managedComment.getStatus() == Status.DISABLED) {
            throw new IllegalArgumentException("Comment is deleted");
        }

        if (managedComment.getUser() == null || !managedComment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to perform this action");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Comment body cannot be empty");
        }


        managedComment.setBody(body);
        return commentRepository.save(managedComment);
    }

    @Transactional
    public void deleteComment(User user, Comment comment) {
        requireAuthenticated(user);
        Comment managedComment =  commentRepository.findByIdForUpdate(comment.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (managedComment.getStatus() == Status.DISABLED) {
            throw new IllegalArgumentException("Comment is deleted");
        }
        if (managedComment.getUser() == null || !managedComment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not permitted to perform this action");
        }
        managedComment.setStatus(Status.DISABLED);
        managedComment.setBody("[deleted]");
        commentRepository.save(managedComment);
    }

    public Map<UUID, Integer> findVotesByPostAndUser(Post post, User user) {
        if (user == null) {
            return Map.of();
        }
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
