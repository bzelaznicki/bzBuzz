package com.zelaznicki.bzBuzz.user;

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

    @GetMapping("/u/{name}")
    public String userProfilePage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            @RequestParam(defaultValue = "NEW") PostSort sort,
            @RequestParam(defaultValue = "0")  int page,
            Model model
            ) {

        User currentUser = userService.findByUserDetails(userDetails);

        User user = userService.findByUsername(name);

        Page<Post> posts = postService.findByUser(user, sort, page);

        Map<UUID, Long> commentCounts = postService.getCommentCounts(posts.getContent());


        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("commentCounts", commentCounts);
        model.addAttribute("currentPage", posts.getNumber());
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("currentSort", sort);

        return "user/profile";
    }
}
