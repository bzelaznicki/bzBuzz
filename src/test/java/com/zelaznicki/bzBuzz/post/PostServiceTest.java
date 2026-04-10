package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.common.ResourceNotFoundException;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock private PostVoteRepository postVoteRepository;
    @Mock private BoardService boardService;

    @InjectMocks
    private PostService postService;

    private User user;
    private User otherUser;
    private Post textPost;
    private Post urlPost;
    private Board board;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otherUser")
                .email("other@example.com")
                .build();

        textPost = Post.builder()
                .id(UUID.randomUUID())
                .voteScore(0)
                .title("My Post")
                .slug("my-post-1a2b3c")
                .status(Status.ENABLED)
                .creator(user)
                .board(board)
                .text("Here's some text")
                .postType(PostType.TEXT)
                .build();

        urlPost = Post.builder()
                .id(UUID.randomUUID())
                .voteScore(0)
                .title("My Post")
                .slug("my-post-1a2b3c")
                .status(Status.ENABLED)
                .creator(user)
                .board(board)
                .postType(PostType.URL)
                .url("https://example.com")
                .build();

        board = Board.builder()
                .id(UUID.randomUUID())
                .name("test")
                .createdBy(user)
                .isPrivate(false)
                .build();
    }

    @Test
    void vote_shouldCreateNewVote_whenNoExistingVote() {
        when(postRepository.findByIdForUpdate(textPost.getId()))
                .thenReturn(Optional.of(textPost));
        when(postVoteRepository.findByPostAndUser(textPost, user))
                .thenReturn(Optional.empty());

        VoteResponse response = postService.vote(textPost, user, 1);

        assertThat(response.voteScore()).isEqualTo(1);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(postVoteRepository).save(any(PostVote.class));
        verify(postRepository).adjustVoteScore(textPost.getId(), 1);
    }

    @Test
    void vote_shouldDeleteVote_whenExistingVote() {
        PostVote existingVote = PostVote.builder()
                .id(UUID.randomUUID())
                .post(textPost)
                .user(user)
                .voteType(1)
                .build();
        when(postRepository.findByIdForUpdate(textPost.getId()))
                .thenReturn(Optional.of(textPost));
        when(postVoteRepository.findByPostAndUser(textPost, user))
                .thenReturn(Optional.of(existingVote));

        VoteResponse response = postService.vote(textPost, user, 1);

        assertThat(response.voteScore()).isEqualTo(-1);
        assertThat(response.action()).isEqualTo("unvoted");

        verify(postVoteRepository).delete(existingVote);
        verify(postRepository).adjustVoteScore(textPost.getId(), -1);
    }

    @Test
    void vote_shouldSwitchVote_whenDifferentVoteTypeExists() {
        PostVote existingVote = PostVote.builder()
                .id(UUID.randomUUID())
                .post(textPost)
                .user(user)
                .voteType(-1)
                .build();

        when(postRepository.findByIdForUpdate(textPost.getId()))
                .thenReturn(Optional.of(textPost));
        when(postVoteRepository.findByPostAndUser(textPost, user))
                .thenReturn(Optional.of(existingVote));

        VoteResponse response = postService.vote(textPost, user, 1);

        assertThat(response.voteScore()).isEqualTo(2);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(postVoteRepository).save(existingVote);
        verify(postRepository).adjustVoteScore(textPost.getId(), 2);
    }

    @Test
    void vote_shouldThrowException_whenInvalidVoteType() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postService.vote(textPost, user, 0)
        );

        assertThat(ex).hasMessage("Invalid vote");
        verifyNoMoreInteractions(postRepository, postVoteRepository);
    }

    @Test
    void vote_shouldThrowException_whenPostIsDeleted() {
        Post deletedPost = Post.builder()
                .id(UUID.randomUUID())
                .voteScore(0)
                .status(Status.DISABLED)
                .build();

        when(postRepository.findByIdForUpdate(deletedPost.getId()))
                .thenReturn(Optional.of(deletedPost));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postService.vote(deletedPost, user, 1)
        );

        assertThat(ex).hasMessage("Post is deleted");
        verifyNoInteractions(postVoteRepository);
        verify(postRepository, never()).adjustVoteScore(deletedPost.getId(), 1);
    }

    @Test
    void post_shouldThrowException_whenTitleIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user,board, "", "Content", PostType.TEXT, null));
        assertThat(ex).hasMessage("Title cannot be empty");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenTitleIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user,board, null, "Content", PostType.TEXT, null)
        );
        assertThat(ex).hasMessage("Title cannot be empty");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenTitleIsOver255Characters() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "this title is over 255 characters, which means that it should be rejected. 255 characters is actually a lot of characters, so the user would really have to try to reach that limit. we should still test that this throws, otherwise we'd be getting into trouble", "The content is short, though", PostType.TEXT, null)
        );

        assertThat(ex).hasMessage("Title cannot exceed 255 characters");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenPostTypeIsTextAndTheBodyIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "New Post", "", PostType.TEXT, null)
        );

        assertThat(ex).hasMessage("Text posts must have a body");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenPostTypeIsTextAndTheBodyIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "New Post", null, PostType.TEXT, null)
        );

        assertThat(ex).hasMessage("Text posts must have a body");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenPostTypeIsURLAndTheURLIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "New Post", null, PostType.URL, "")
        );

        assertThat(ex).hasMessage("URL posts must have a URL");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenPostTypeIsURLAndTheURLIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "New Post", null, PostType.URL, null)
        );

        assertThat(ex).hasMessage("URL posts must have a URL");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenBothURLAndTextAreProvided() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user, board, "New Post", "Hey, here's some text", PostType.TEXT, "https://example.com")
        );

        assertThat(ex).hasMessage("A post cannot have both text and a URL");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenURLIsJavaScript() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user,board, "Cool new link", null, PostType.URL, "javascript:alert('Hello, world!')")
        );

        assertThat(ex).hasMessage("Invalid URL");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenURLIsData() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.create(user,board, "New Post", null, PostType.URL, "data:asdasd")
        );

        assertThat(ex).hasMessage("Invalid URL");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldCreateTextPost_whenTextIsProvided() {
        String postTitle = "New Post";
        String postBody =  "Hey, here's some text";

        postService.create(user, board, postTitle, postBody, PostType.TEXT, null);

        verify(postRepository).save(argThat(p ->
                        p.getTitle().equals(postTitle) &&
                        p.getStatus() == Status.ENABLED &&
                        p.getVoteScore() == 0 &&
                        p.getUrl() == null &&
                        p.getText().equals(postBody)
        ));
    }

    @Test
    void post_shouldCreateUrlPost_whenURLIsProvided() {
        String postTitle = "New Post";
        String postUrl =  "https://example.com";

        postService.create(user, board, postTitle, null, PostType.URL, postUrl);

        verify(postRepository).save(argThat(p ->
                        p.getTitle().equals(postTitle) &&
                        p.getStatus() == Status.ENABLED &&
                        p.getVoteScore() == 0 &&
                        p.getUrl().equals(postUrl) &&
                        p.getText() == null &&
                        p.getSlug() != null
                ));
    }

    @Test
    void post_shouldThrowException_whenDeletedSlugIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.deletePost("", user)
        );

        assertThat(ex).hasMessage("Slug cannot be empty");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenDeletedSlugIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.deletePost(null, user)
        );

        assertThat(ex).hasMessage("Slug cannot be empty");
        verifyNoInteractions(postRepository);
    }

    @Test
    void post_shouldThrowException_whenPostIsNotFound() {

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.deletePost("random-slug-1a2b3c", user)
        );

        assertThat(ex).hasMessage("Post not found");
    }

    @Test
    void shouldThrowException_whenUnauthorizedUserDeletesPost() {
        when(postRepository.findBySlugAndStatus(textPost.getSlug(), Status.ENABLED))
                .thenReturn(Optional.of(textPost));
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.deletePost(textPost.getSlug(), otherUser)
        );

        assertThat(ex).hasMessage("You are not authorized to perform this action");
    }

    @Test
    void shouldDeletePost_whenPostIsFoundAndDeletedByOwner() {
        when(postRepository.findBySlugAndStatus(textPost.getSlug(), Status.ENABLED))
        .thenReturn(Optional.of(textPost));
        postService.deletePost(textPost.getSlug(), user);

        verify(postRepository).save(argThat(
                p ->
                        p.getStatus() == Status.DISABLED
                )
        );
    }

    
}
