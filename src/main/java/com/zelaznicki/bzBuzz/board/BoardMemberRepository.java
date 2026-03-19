package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bm FROM BoardMember bm WHERE bm.board = :board AND bm.user = :user")
    Optional<BoardMember> findByBoardAndUserForUpdate(@Param("board") Board board, @Param("user") User user);
    @Modifying
    @Transactional
    void deleteByBoardAndUser(Board board, User user);

}
