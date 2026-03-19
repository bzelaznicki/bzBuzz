package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {

    Optional<BoardMember> findByBoardAndUser(Board board, User user);
    boolean existsByBoardAndUser(Board board, User user);
    List<BoardMember> findByBoard(Board board);
    List<BoardMember> findByUser(User user);

    long countByBoardAndRole(Board board, MembershipRole role);

    @Modifying
    @Transactional
    void deleteByBoardAndUser(Board board, User user);

    @Modifying
    BoardMember updateByBoardAndUser(Board board, User user);
}
