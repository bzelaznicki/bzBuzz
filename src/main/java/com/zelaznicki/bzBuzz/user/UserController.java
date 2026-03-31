package com.zelaznicki.bzBuzz.user;

import com.zelaznicki.bzBuzz.board.BoardMember;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.comment.Comment;
import com.zelaznicki.bzBuzz.comment.CommentService;
import com.zelaznicki.bzBuzz.common.PostSort;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final BoardService boardService;

    @GetMapping("/u/{name}")
    public String userProfilePage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            @RequestParam(defaultValue = "posts") String tab,
            @RequestParam(defaultValue = "NEW") PostSort postSort,
            @RequestParam(defaultValue = "NEW") PostSort commentSort,
            @RequestParam(defaultValue = "0") int postPage,
            @RequestParam(defaultValue = "0") int commentPage,
            Model model
    ) {
        User currentUser = userService.findByUserDetails(userDetails);
        User user = userService.findByUsername(name);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", user);
        model.addAttribute("tab", tab);

        if (tab.equals("posts")) {
            Page<Post> posts = postService.findByUser(user, postSort, postPage);
            Map<UUID, Long> commentCounts = postService.getCommentCounts(posts.getContent());
            model.addAttribute("posts", posts);
            model.addAttribute("commentCounts", commentCounts);
            model.addAttribute("currentPostPage", posts.getNumber());
            model.addAttribute("totalPostPages", posts.getTotalPages());
            model.addAttribute("currentPostSort", postSort);

        } else if (tab.equals("comments")) {
            Page<Comment> comments = commentService.findByUser(user, commentSort, commentPage);
            model.addAttribute("userComments", comments);
            model.addAttribute("currentCommentPage", comments.getNumber());
            model.addAttribute("totalCommentPages", comments.getTotalPages());
            model.addAttribute("currentCommentSort", commentSort);

        } else if (tab.equals("boards")) {
            List<BoardMember> boards = boardService.findBoardsByUser(user);
            model.addAttribute("boards", boards);
        }

        return "user/profile";
    }
}
