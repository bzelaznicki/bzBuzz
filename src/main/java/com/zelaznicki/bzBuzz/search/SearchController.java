package com.zelaznicki.bzBuzz.search;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostService;
import com.zelaznicki.bzBuzz.user.User;
import com.zelaznicki.bzBuzz.user.UserService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;
    private final PostService postService;

    @GetMapping("/search")
    public String searchResultsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            Model model
            ) {
        User currentUser = userService.findByUserDetails(userDetails);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("query", q);

        List<Board> boards = searchService.searchBoards(q);
        Page<Post> posts = searchService.findPostsByTitle(q, page);

        Map<UUID, Long> commentCounts = postService.getCommentCounts(posts.getContent());

        model.addAttribute("posts", posts);
        model.addAttribute("commentCounts", commentCounts);
        model.addAttribute("boards", boards);
        model.addAttribute("currentPage", posts.getNumber());
        model.addAttribute("totalPages", posts.getTotalPages());

        return "search/results";
    }
}
