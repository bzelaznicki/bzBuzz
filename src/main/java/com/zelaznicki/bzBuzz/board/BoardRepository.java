package com.zelaznicki.bzBuzz.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    Optional<Board> findByName(String name);
    List<Board> findAllByOrderByMemberCountDesc();
    List<Board> findByIsPrivateFalseOrderByMemberCountDesc();
    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Board b SET b.memberCount = b.memberCount + 1 WHERE b.id = :id")
    void incrementMemberCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Board b SET b.memberCount = b.memberCount - 1 WHERE b.id = :id")
    void decrementMemberCount(@Param("id") UUID id);
}
