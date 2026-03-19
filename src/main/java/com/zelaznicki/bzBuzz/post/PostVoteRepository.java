package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {

    Optional<PostVote> findByPostAndUser(Post post, User user);
    boolean existsByPostAndUser(Post post, User user);

    @Modifying
    @Transactional
    void deleteByPostAndUser(Post post, User user);

}
