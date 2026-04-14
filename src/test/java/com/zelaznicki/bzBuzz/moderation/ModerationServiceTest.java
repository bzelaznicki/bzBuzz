package com.zelaznicki.bzBuzz.moderation;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardMember;
import com.zelaznicki.bzBuzz.board.BoardMemberRepository;
import com.zelaznicki.bzBuzz.board.MembershipRole;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostRepository;
import com.zelaznicki.bzBuzz.post.PostType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModerationServiceTest {
    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private ModerationService moderationService;

    private User moderator;
    private User regularUser;
    private Board board;
    private Post post;


    @BeforeEach
    void setUp() {
        moderator = User.builder()
                .id(UUID.randomUUID())
                .username("modUser")
                .email("mod@example.com")
                .build();

        regularUser = User.builder()
                .id(UUID.randomUUID())
                .username("regularUser")
                .email("regular@example.com")
                .build();

        board = Board.builder()
                .id(UUID.randomUUID())
                .name("java")
                .memberCount(2).createdBy(moderator)
                .isPrivate(false)
                .build();

        post = Post.builder()
                .id(UUID.randomUUID())
                .creator(moderator)
                .board(board)
                .title("Cool Post!")
                .slug("cool-post-a1b2c3")
                .text("So cool!")
                .postType(PostType.TEXT)
                .voteScore(1)
                .status(Status.ENABLED)
                .build();
    }

    @Test
    void mod_shouldThrowException_whenDeletingPostWhenUserIsNotAModerator() {
        when(boardMemberRepository.findByBoardAndUser(board,regularUser))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> moderationService.removePost(post, regularUser, board));

       assertThat(ex).hasMessage("You are not a moderator of this board");
       verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void mod_shouldRemovePost_whenUserIsModerator() {
        BoardMember moderatorMember = BoardMember.builder()
                .id(UUID.randomUUID())
                .user(moderator)
                .board(board)
                .role(MembershipRole.MODERATOR)
                .build();

        when(boardMemberRepository.findByBoardAndUser(board,moderator))
            .thenReturn(Optional.of(moderatorMember));

        moderationService.removePost(post, moderator, board);

        verify(postRepository).save(argThat(
                p ->
                        p.getStatus().equals(Status.REMOVED)
        ));
    }
}
