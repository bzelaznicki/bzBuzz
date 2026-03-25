package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.common.ResourceNotFoundException;
import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;

    private boolean validateBoardName(String name) {
        return !name.isBlank() && name.matches("[a-z0-9_-]{1,100}");
    }

    @Transactional
    public Board create(String name, String description, String bannerUrl, User user, boolean isPrivate) {
        //Rules for the board name:
        //1. alphanumeric + - and _
        //2. lowercase
        //3. 1-100 characters

        String normalizedName = name == null ? "" : name.trim().toLowerCase();

        if (!validateBoardName(normalizedName)) {
            throw new IllegalArgumentException("Board name must contain 1-100 alphanumeric characters. Underscores and dashes are permitted.");
        }
        if (boardRepository.existsByName(normalizedName)) {
            throw new IllegalArgumentException("Board name already exists");
        }

        Board board = Board.builder()
                .name(normalizedName)
                .description(description)
                .bannerUrl(bannerUrl)
                .createdBy(user)
                .memberCount(1)
                .isPrivate(isPrivate).build();

        BoardMember boardMember = BoardMember.builder()
                .user(user)
                .board(board)
                .role(MembershipRole.MODERATOR)
                .build();

        boardRepository.save(board);
        boardMemberRepository.save(boardMember);

        return board;
    }

    public Board findByName(String name) {
        String normalizedName = name == null ? "" : name.trim().toLowerCase();

        if (!validateBoardName(normalizedName)) {
            throw new IllegalArgumentException("Invalid board name");
        }
        return boardRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
    }

    public List<Board> findPublicBoards() {
        return boardRepository.findByIsPrivateFalseOrderByMemberCountDesc();
    }

    public List<BoardMember> findBoardsByUser(User user) {
        return boardMemberRepository.findByUser(user);
    }

    public boolean isMember(Board board, User user) {
        return boardMemberRepository.existsByBoardAndUser(board, user);
    }

    @Transactional
    public BoardMember joinBoard(Board board, User user) {
        if (boardMemberRepository.existsByBoardAndUser(board, user)) {
            throw new IllegalArgumentException("User is already a member of this board");
        }

        if (board.isPrivate()) {
            throw new IllegalArgumentException("Board is private");
        }

        BoardMember boardMember = BoardMember.builder()
                .user(user)
                .board(board)
                .role(MembershipRole.MEMBER)
                .build();

        boardRepository.incrementMemberCount(board.getId());

        return boardMemberRepository.save(boardMember);
    }

    @Transactional
    public BoardMember changeMemberRole(Board board, User user, MembershipRole role) {
        Board lockedBoard = boardRepository.findByIdForUpdate(board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        BoardMember member = boardMemberRepository.findByBoardAndUserForUpdate(lockedBoard, user)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this board"));

        if (role == MembershipRole.MEMBER) {
            long moderatorCount = boardMemberRepository.countByBoardAndRole(lockedBoard, MembershipRole.MODERATOR);
            if (moderatorCount <= 1 && member.getRole() == MembershipRole.MODERATOR) {
                throw new IllegalArgumentException("Cannot demote the last moderator");
            }
        }
        member.setRole(role);
        return boardMemberRepository.save(member);
    }

    @Transactional
    public void removeMemberFromBoard(Board board, User user) {
        Board lockedBoard = boardRepository.findByIdForUpdate(board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        BoardMember member = boardMemberRepository.findByBoardAndUserForUpdate(lockedBoard, user)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this board"));

        if (member.getRole() == MembershipRole.MODERATOR && boardMemberRepository.countByBoardAndRole(lockedBoard, MembershipRole.MODERATOR) < 2) {
            throw new IllegalArgumentException("A board cannot have 0 moderators");
        }

        boardMemberRepository.deleteByBoardAndUser(lockedBoard, user);
        boardRepository.decrementMemberCount(lockedBoard.getId());
    }

    /**
     * Load a Board by name and enforce access rules for private boards.
     *
     * @param boardName      the board's unique name
     * @param user           the current authenticated user, or null for anonymous access
     * @return the resolved Board when found and accessible to the caller
     * @throws ResponseStatusException with status 404 if the board name is unknown, or 403 if the board is private and the user is not a member
     */
    public Board getBoardAndCheckAccess(String boardName, User user) {
        try {
            Board board = findByName(boardName);
            boolean isMember = user != null && isMember(board, user);
            if (board.isPrivate() && !isMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return board;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
