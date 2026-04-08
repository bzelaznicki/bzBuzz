package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.BoardService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock private PostVoteRepository postVoteRepository;
    @Mock private BoardService boardService;

    @InjectMocks
    private PostService postService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        post = Post.builder()
                .id(UUID.randomUUID())
                .voteScore(0)
                .status(Status.ENABLED)
                .build();


    }

    @Test
    void vote_shouldCreateNewVote_whenNoExistingVote() {
        when(postRepository.findByIdForUpdate(post.getId()))
                .thenReturn(Optional.of(post));
        when(postVoteRepository.findByPostAndUser(post, user))
                .thenReturn(Optional.empty());

        VoteResponse response = postService.vote(post, user, 1);

        assertThat(response.voteScore()).isEqualTo(1);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(postVoteRepository).save(any(PostVote.class));
        verify(postRepository).adjustVoteScore(post.getId(), 1);
    }

    @Test
    void vote_shouldDeleteVote_whenExistingVote() {
        PostVote existingVote = PostVote.builder()
                .id(UUID.randomUUID())
                .post(post)
                .user(user)
                .voteType(1)
                .build();
        when(postRepository.findByIdForUpdate(post.getId()))
                .thenReturn(Optional.of(post));
        when(postVoteRepository.findByPostAndUser(post, user))
                .thenReturn(Optional.of(existingVote));

        VoteResponse response = postService.vote(post, user, 1);

        assertThat(response.voteScore()).isEqualTo(-1);
        assertThat(response.action()).isEqualTo("unvoted");

        verify(postVoteRepository).delete(existingVote);
        verify(postRepository).adjustVoteScore(post.getId(), -1);
    }

    @Test
    void vote_shouldSwitchVote_whenDifferentVoteTypeExists() {
        PostVote existingVote = PostVote.builder()
                .id(UUID.randomUUID())
                .post(post)
                .user(user)
                .voteType(-1)
                .build();

        when(postRepository.findByIdForUpdate(post.getId()))
                .thenReturn(Optional.of(post));
        when(postVoteRepository.findByPostAndUser(post, user))
                .thenReturn(Optional.of(existingVote));

        VoteResponse response = postService.vote(post, user, 1);

        assertThat(response.voteScore()).isEqualTo(2);
        assertThat(response.action()).isEqualTo("upvoted");

        verify(postVoteRepository).save(existingVote);
        verify(postRepository).adjustVoteScore(post.getId(), 2);
    }

    @Test
    void vote_shouldThrowException_whenInvalidVoteType() {

        assertThrows(IllegalArgumentException.class, () ->
                postService.vote(post, user, 0)
        );
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

        assertThrows(IllegalArgumentException.class, () ->
                postService.vote(deletedPost, user, 1)
        );
    }
}
