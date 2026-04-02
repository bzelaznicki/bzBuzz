package com.zelaznicki.bzBuzz.search;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardRepository;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;

    private static final int PAGE_SIZE = 25;

    public List<Board> searchBoards(String query) {
        if (query == null || query.isBlank()) return List.of();
        return boardRepository.searchPublicBoards(query);
    }


    public Page<Post> findPostsByTitle(String query, int page) {
        if (query == null || query.isBlank() || page < 0) return Page.empty();
        return postRepository.findByTitleContainingIgnoreCaseAndStatus(query, Status.ENABLED, PageRequest.of(page, PAGE_SIZE));
    }
}
