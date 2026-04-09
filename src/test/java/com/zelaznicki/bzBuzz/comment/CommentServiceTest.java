package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostType;
import com.zelaznicki.bzBuzz.post.VoteResponse;
import com.zelaznicki.bzBuzz.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentVoteRepository commentVoteRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment comment;
    private User user;
    private Post post;
    private Board board;

    private CommentVote getCommentVote(int delta) {
        return CommentVote.builder()
                .id(UUID.randomUUID())
                .user(user)
                .comment(comment)
                .voteType(delta)
                .build();
    }

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();
        board = Board.builder()
                .id(UUID.randomUUID())
                .createdBy(user)
                .name("test")
                .memberCount(1)
                .build();

        post = Post.builder()
                .id(UUID.randomUUID())
                .voteScore(0)
                .status(Status.ENABLED)
                .creator(user)
                .postType(PostType.TEXT)
                .text("This is the text content")
                .board(board)
                .title("This is a post")
                .slug("this-is-a-post-abc123")
                .build();

        comment = Comment.builder()
                .id(UUID.randomUUID())
                .user(user)
                .post(post)
                .voteScore(0)
                .status(Status.ENABLED)
                .body("Hello, world!")
                .build();


    }

    @Test
    void comment_shouldThrowException_whenBodyIsEmptyOnNewComment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> commentService.addComment(user, post, null, ""));

        assertThat(ex).hasMessage("Comment body cannot be empty");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldThrowException_whenBodyIsNullOnNewComment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> commentService.addComment(user, post, comment, null));
        assertThat(ex).hasMessage("Comment body cannot be empty");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldThrowException_whenAddingCommentToDeletedPost() {
        post.setStatus(Status.DISABLED);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(user, post, null, "Hello, world!"));

        assertThat(ex).hasMessage("Cannot comment on a deleted post");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldThrowException_whenUserIsNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                commentService.addComment(null, post, null, "Hello"));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getReason()).isEqualTo("Authentication required");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldThrowException_whenReplyingToACommentFromADifferentPost() {
        Post differentPost = Post.builder()
                .id(UUID.randomUUID())
                .creator(user)
                .board(board)
                .title("Definitely not the same post")
                .slug("definitely-not-the-same-post-xyz321")
                .postType(PostType.URL)
                .url("https://example.com")
                .voteScore(0)
                .status(Status.ENABLED)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(user, differentPost, comment, "Hello, world!")
                );

        assertThat(ex).hasMessage("Reply parent must belong to the same post");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldThrowException_whenAddingReplyToDeletedComment() {
        comment.setStatus(Status.DISABLED);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(user, post, comment, "Hello, world!"));

        assertThat(ex).hasMessage("Cannot reply to a deleted comment");
        verifyNoInteractions(commentRepository);
    }

    @Test
    void comment_shouldCreateComment_whenBodyIsValid() {
        String commentBody = "Hello, world!";
        commentService.addComment(user, post, null, commentBody);

        verify(commentRepository).save(argThat(
                c ->
                        c.getStatus() == Status.ENABLED &&
                                c.getVoteScore() == 0 &&
                                c.getBody().equals(commentBody)
        ));
    }

    @Test
    void comment_shouldCreateReply_whenParentCommentProvided() {
        commentService.addComment(user, post, comment, "This is a reply");

        verify(commentRepository).save(argThat(c ->
                c.getParent() != null &&
                        c.getParent().getId().equals(comment.getId())
        ));
    }

    @Test
    void comment_shouldCreateNewVote_whenNoExistingVote() {
        when(commentRepository.findByIdForUpdate(comment.getId()))
                .thenReturn(Optional.of(comment));
        when(commentVoteRepository.findByCommentAndUser(comment, user))
                .thenReturn(Optional.empty());

        VoteResponse response = commentService.vote(comment, user, 1);

        assertThat(response.voteScore()).isEqualTo(1);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(commentVoteRepository).saveAndFlush(any(CommentVote.class));
        verify(commentRepository).adjustVoteScore(comment.getId(), 1);
    }

    @Test
    void comment_shouldDeleteVote_whenVoteExists() {
        CommentVote vote = getCommentVote(1);

        when(commentRepository.findByIdForUpdate(comment.getId()))
                .thenReturn(Optional.of(comment));

        when(commentVoteRepository.findByCommentAndUser(comment, user))
                .thenReturn(Optional.of(vote));

        VoteResponse response = commentService.vote(comment, user, 1);

        assertThat(response.voteScore()).isEqualTo(-1);
        assertThat(response.action()).isEqualTo("unvoted");

        verify(commentVoteRepository).delete(vote);
        verify(commentRepository).adjustVoteScore(comment.getId(), -1);
    }

    @Test
    void comment_shouldReplaceVote_whenChangingVoteType() {
        CommentVote vote = getCommentVote(-1);

        when(commentRepository.findByIdForUpdate(comment.getId()))
                .thenReturn(Optional.of(comment));
        when(commentVoteRepository.findByCommentAndUser(comment, user))
                .thenReturn(Optional.of(vote));

        VoteResponse response = commentService.vote(comment, user, 1);

        assertThat(response.voteScore()).isEqualTo(2);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(commentVoteRepository).save(vote);
        verify(commentRepository).adjustVoteScore(comment.getId(), 2);
    }

    @Test
    void comment_shouldThrowException_whenInvalidVoteIsCast() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.vote(comment,user,2)
        );
        assertThat(ex).hasMessage("Invalid vote");
        verifyNoInteractions(commentRepository);
        verifyNoInteractions(commentVoteRepository);
    }

    @Test
    void comment_shouldThrowException_whenVotingOnDeletedComment() {
        comment.setStatus(Status.DISABLED);
        when(commentRepository.findByIdForUpdate(comment.getId()))
        .thenReturn(Optional.of(comment));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.vote(comment, user, 1)
        );

        assertThat(ex).hasMessage("Comment is deleted");
        verifyNoInteractions(commentVoteRepository);
    }

    
}
