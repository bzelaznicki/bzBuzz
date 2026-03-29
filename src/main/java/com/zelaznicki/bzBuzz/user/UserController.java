package com.zelaznicki.bzBuzz.user;

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

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @GetMapping("/u/{name}")
    public String userProfilePage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            @RequestParam(defaultValue = "NEW") PostSort postSort,
            @RequestParam(defaultValue = "NEW") PostSort commentSort,
            @RequestParam(defaultValue = "0")  int postPage,
            @RequestParam(defaultValue = "0") int commentPage,
            Model model
            ) {

        User currentUser = userService.findByUserDetails(userDetails);

        User user = userService.findByUsername(name);

        Page<Post> posts = postService.findByUser(user, postSort, postPage);
        Map<UUID, Long> commentCounts = postService.getCommentCounts(posts.getContent());

        Page<Comment> userComments = commentService.findByUser(user, commentSort, commentPage);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("commentCounts", commentCounts);
        model.addAttribute("currentPostPage", posts.getNumber());
        model.addAttribute("totalPostPages", posts.getTotalPages());
        model.addAttribute("currentPostSort", postSort);
        model.addAttribute("userComments", userComments);
        model.addAttribute("currentCommentPage", userComments.getNumber());
        model.addAttribute("totalCommentPages",  userComments.getTotalPages());
        model.addAttribute("currentCommentSort", commentSort);;

        return "user/profile";
    }
}
