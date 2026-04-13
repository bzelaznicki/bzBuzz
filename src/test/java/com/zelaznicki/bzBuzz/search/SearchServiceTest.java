package com.zelaznicki.bzBuzz.search;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardRepository;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostRepository;
import com.zelaznicki.bzBuzz.post.PostType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void search_shouldReturnEmptyBoardList_whenBoardQueryIsNull() {
        List<Board> found = searchService.searchBoards(null);

        assertThat(found).isEqualTo(Collections.emptyList());
        verifyNoInteractions(boardRepository);
    }

    @Test
    void search_shouldReturnEmptyBoardList_whenBoardQueryIsEmpty() {
        List<Board> found = searchService.searchBoards("");

        assertThat(found).isEqualTo(Collections.emptyList());
        verifyNoInteractions(boardRepository);
    }

    @Test
    void search_shouldReturnEmptyBoardList_whenBoardQueryIsBlank() {
        List<Board> found = searchService.searchBoards("       ");

        assertThat(found).isEqualTo(Collections.emptyList());
        verifyNoInteractions(boardRepository);
    }

    @Test
    void search_shouldEscapeSpecialCharacters_whenBoardQueryContainsWildCards() {
        String rawQuery = "Bobb\\y_T%ables_";
        String escapedQuery = rawQuery
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        searchService.searchBoards(rawQuery);

        verify(boardRepository).searchPublicBoards(escapedQuery);
    }

    @Test
    void search_shouldReturnBoardResults_whenQueryIsValid() {
        Board board = Board.builder().id(UUID.randomUUID()).name("java").build();
        when(boardRepository.searchPublicBoards("java"))
                .thenReturn(List.of(board));

        List<Board> result = searchService.searchBoards("java");
        assertThat(result).isEqualTo(List.of(board));
        verify(boardRepository).searchPublicBoards("java");
    }

    @Test
    void search_shouldReturnEmptyPostPage_whenPostQueryIsNull() {
        Page<Post> found = searchService.findPostsByTitle(null, 0);

        assertThat(found).isEqualTo(Page.empty());
        verifyNoInteractions(postRepository);
    }

    @Test
    void search_shouldReturnEmptyPostPage_whenPostQueryIsEmpty() {
        Page<Post> found = searchService.findPostsByTitle("", 0);

        assertThat(found).isEqualTo(Page.empty());
        verifyNoInteractions(postRepository);
    }

    @Test
    void search_shouldReturnEmptyPostPage_whenPostQueryIsBlank() {
        Page<Post> found = searchService.findPostsByTitle("             ", 0);

        assertThat(found).isEqualTo(Page.empty());
        verifyNoInteractions(postRepository);
    }

    @Test
    void search_shouldReturnPosts_whenPostQueryIsValid() {
        Post post = Post.builder().id(UUID.randomUUID())
                .title("Java stuff")
                .text("Hello, world!").postType(PostType.TEXT)
                .build();

        Page<Post> page = new PageImpl<>(List.of(post));

        when(postRepository.findByTitleContainingIgnoreCaseAndStatus(
                "java",
                Status.ENABLED,
                PageRequest.of(0, 25)))
                .thenReturn(page);

        Page<Post> found = searchService.findPostsByTitle("java", 0);

        assertThat(found.getContent()).isEqualTo(List.of(post));
        verify(postRepository).findByTitleContainingIgnoreCaseAndStatus(
                "java",
                Status.ENABLED,
                PageRequest.of(0, 25)
        );
    }

    @Test
    void search_shouldReturnEmptyPostPage_whenPageIsNegative() {
        Page<Post> found = searchService.findPostsByTitle("java", -1);

        assertThat(found).isEqualTo(Page.empty());
        verifyNoInteractions(postRepository);
    }


}
