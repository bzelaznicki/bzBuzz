package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.user.User;
import com.zelaznicki.bzBuzz.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final BoardService boardService;
    private final UserService userService;

    private Board getBoardAndCheckAccess(String boardName, User user) {
        try {
            Board board = boardService.findByName(boardName);
            boolean isMember = user != null && boardService.isMember(board, user);
            if (board.isPrivate() && !isMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return board;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/b/{boardName}/posts/{slug}")
    public String postPage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String boardName, @PathVariable String slug, Model model) {
        User user = userService.findByUserDetails(userDetails);
        Board board = getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        Post post = postService.findBySlug(slug);
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("post", post);
        model.addAttribute("isMember", isMember);
        return "post/view";
    }

    @GetMapping("/b/{boardName}/submit")
    public String submitPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            Model model
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("isMember", isMember);

        return "post/create";

    }

    @GetMapping("/b/{boardName}/posts/{slug}/edit")
    public String editPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            Model model
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        Post post = postService.findBySlug(slug);
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("post", post);
        model.addAttribute("isMember", isMember);

        return "post/edit";
    }


    @PostMapping("/b/{boardName}/submit")
    public String submitPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @RequestParam String title,
            @RequestParam(required = false) String text,
            @RequestParam PostType postType,
            @RequestParam(required = false) String url,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.findByUserDetails(userDetails);

            Board board = boardService.findByName(boardName);

            boolean isMember = user != null && boardService.isMember(board, user);
            if (board.isPrivate() && !isMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Post post = postService.create(user, board, title, text, postType, url);

            redirectAttributes.addFlashAttribute("successMessage", "Post created");
            return "redirect:/b/" + boardName + "/posts/" + post.getSlug();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName;
        }
    }

    @PostMapping("/b/{boardName}/posts/{slug}/delete")
    public String deletePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.findByUserDetails(userDetails);

            postService.deletePost(slug, user);

            redirectAttributes.addFlashAttribute("successMessage", "Post deleted");
            return "redirect:/b/" + boardName;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName + "/posts/" + slug;
        }
    }

    @PostMapping("/b/{boardName}/posts/{slug}/vote")
    public String vote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @RequestParam int voteType,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User user =  userService.findByUserDetails(userDetails);
            Post post =  postService.findBySlug(slug);

            postService.vote(post, user, voteType);

            redirectAttributes.addFlashAttribute("successMessage", "Voted successfully");
            return "redirect:/b/" + boardName + "/posts/" + post.getSlug();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName + "/posts/" + slug;
        }
    }

    @PostMapping("/b/{boardName}/posts/{slug}/edit")
    public String editPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @RequestParam String title,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String url,
            @PathVariable String slug,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.findByUserDetails(userDetails);
            Post post = postService.updatePost(slug, user, title, text, url);

            redirectAttributes.addFlashAttribute("successMessage", "Post updated");
            return "redirect:/b/" + boardName + "/posts/" + post.getSlug();

        }  catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName + "/posts/" + slug;
        }
    }
}
