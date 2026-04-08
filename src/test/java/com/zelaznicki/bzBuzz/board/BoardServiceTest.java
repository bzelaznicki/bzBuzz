package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.common.ResourceNotFoundException;
import com.zelaznicki.bzBuzz.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @InjectMocks
    private BoardService boardService;

    private User user;
    private Board board;

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
    }

    @Test
    void board_shouldThrowException_whenLeavingAsNotAMember() {

        when(boardMemberRepository.findByBoardAndUserForUpdate(board,user))
                .thenReturn(Optional.empty());
        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));

        assertThrows(AccessDeniedException.class, () ->
                boardService.removeMemberFromBoard(board, user));
    }

    @Test
    void board_shouldThrowException_whenLeavingAsLastModerator() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MODERATOR)
                .build();

        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));
        when(boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR))
                .thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () ->
                boardService.removeMemberFromBoard(board, user));
    }

    @Test
    void board_shouldRemoveMemberAndDecrementMemberCount_whenRegularMemberLeaves() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MEMBER)
                .build();

        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));

        boardService.removeMemberFromBoard(board, user);

        verify(boardMemberRepository).deleteByBoardAndUser(board, user);
        verify(boardRepository).decrementMemberCount(board.getId());
    }

    @Test
    void board_shouldRemoveModeratorAndDecrementMemberCount_whenMoreThanOneModeratorExists() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MODERATOR)
                .build();
        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));

        when(boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR))
                .thenReturn(2L);

        boardService.removeMemberFromBoard(board, user);

        verify(boardMemberRepository).deleteByBoardAndUser(board, user);
        verify(boardRepository).decrementMemberCount(board.getId());
    }

    @Test
    void board_shouldThrowException_whenDemotingTheLastModerator() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MODERATOR)
                .build();

        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));
        when(boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR))
                .thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () ->
                boardService.changeMemberRole(board, user, MembershipRole.MEMBER));
    }

    @Test
    void board_shouldDemoteModerator_whenMoreThanOneModeratorExists() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MODERATOR)
                .build();

        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));
        when(boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR))
                .thenReturn(2L);

        boardService.changeMemberRole(board, user, MembershipRole.MEMBER);

        assertThat(member.getRole()).isEqualTo(MembershipRole.MEMBER);
        verify(boardMemberRepository).save(member);
    }

    @Test
    void board_shouldPromoteMemberToModerator() {
        BoardMember member = BoardMember.builder()
                .id(UUID.randomUUID())
                .board(board)
                .user(user)
                .role(MembershipRole.MEMBER)
                .build();

        when(boardRepository.findByIdForUpdate(board.getId()))
                .thenReturn(Optional.of(board));
        when(boardMemberRepository.findByBoardAndUserForUpdate(board, user))
                .thenReturn(Optional.of(member));

        boardService.changeMemberRole(board, user, MembershipRole.MODERATOR);

        assertThat(member.getRole()).isEqualTo(MembershipRole.MODERATOR);
        verify(boardMemberRepository).save(member);
    }

    @Test
    void board_shouldThrowException_whenJoiningAlreadyJoinedBoard() {

        when(boardMemberRepository.existsByBoardAndUser(board, user))
                .thenReturn(true);


        assertThrows(IllegalArgumentException.class, () ->
                boardService.joinBoard(board, user));
    }

    @Test
    void board_shouldThrowException_whenJoiningPrivateBoard() {
        board.setPrivate(true);
        when(boardMemberRepository.existsByBoardAndUser(board,user))
                .thenReturn(false);
        assertThrows(IllegalArgumentException.class, () ->
                boardService.joinBoard(board,user));
    }

    @Test
    void board_shouldCreateMembershipAndIncrementMemberCount_whenJoiningBoard() {

        when(boardMemberRepository.existsByBoardAndUser(board,user))
                .thenReturn(false);


        boardService.joinBoard(board, user);

        verify(boardMemberRepository).save(any(BoardMember.class));
        verify(boardRepository).incrementMemberCount(board.getId());
    }

    @Test
    void board_shouldThrowException_whenCreatedBoardNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                boardService.create("aN!inv4l1dN4m€", "This won't work", "", user, false));
    }

    @Test
    void board_shouldThrowException_whenCreatedBoardNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                boardService.create("", "", "", user, false));
    }

    @Test
    void board_shouldThrowException_whenCreatedBoardNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                boardService.create(null, "This won't work", "", user, false));
    }

    @Test
    void board_shouldThrowException_whenBoardNameAlreadyExists() {
        when(boardRepository.existsByName(board.getName())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                boardService.create("test", "This board already exists", "", user, false));
    }

    @Test
    void board_shouldCreateBoardAndAddUserAsModerator_whenCreatingBoard() {
        when(boardRepository.existsByName(board.getName())).thenReturn(false);

        boardService.create(board.getName(), "This is a new board", "", user, false);

        verify(boardRepository).save(any(Board.class));
        verify(boardMemberRepository).save(any(BoardMember.class));
    }
}
