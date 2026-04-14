package com.zelaznicki.bzBuzz.moderation;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardMemberRepository;
import com.zelaznicki.bzBuzz.board.MembershipRole;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostRepository;
import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final BoardMemberRepository boardMemberRepository;
    private final PostRepository postRepository;

    public void removePost(Post post, User user, Board board) {
        boardMemberRepository.findByBoardAndUser(board, user)
                .filter(m -> m.getRole() == MembershipRole.MODERATOR)
                .orElseThrow(() -> new IllegalArgumentException("You are not a moderator of this board"));
    }
}
