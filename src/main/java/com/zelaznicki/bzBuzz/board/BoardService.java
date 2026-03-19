package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;

    @Transactional
    public Board create(String name, String description, String bannerUrl, User user, boolean isPrivate) {
        if (boardRepository.existsByName(name)) {
            throw new IllegalArgumentException("Board name already exists");
        }

        Board board = Board.builder()
                .name(name)
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
        return boardRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));
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
        BoardMember member = boardMemberRepository.findByBoardAndUser(board, user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this board"));

        if (role == MembershipRole.MEMBER) {
            long moderatorCount = boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR);
            if (moderatorCount <= 1 && member.getRole() == MembershipRole.MODERATOR) {
                throw new IllegalArgumentException("Cannot demote the last moderator");
            }
        }
        member.setRole(role);
        return boardMemberRepository.save(member);
    }

    @Transactional
    public void removeMemberFromBoard(Board board, User user) {
        BoardMember member = boardMemberRepository.findByBoardAndUser(board, user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this board"));

        if (member.getRole() == MembershipRole.MODERATOR && boardMemberRepository.countByBoardAndRole(board, MembershipRole.MODERATOR) < 2) {
            throw new IllegalArgumentException("A board cannot have 0 moderators");
        }

        boardMemberRepository.deleteByBoardAndUser(board, user);
        boardRepository.decrementMemberCount(board.getId());
    }
}
