package com.zelaznicki.bzBuzz.board;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
