package com.zelaznicki.bzBuzz.comment;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostService;
import com.zelaznicki.bzBuzz.post.VoteResponse;
import com.zelaznicki.bzBuzz.user.User;
import com.zelaznicki.bzBuzz.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;
    private final BoardService boardService;

    @PostMapping("/b/{boardName}/posts/{slug}/comments")
    public String addTopLevelComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUserDetails(userDetails);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Board board = boardService.getBoardAndCheckAccess(boardName, user);

        Post post = postService.findByBoardAndSlug(board, slug);


        try {
            commentService.addComment(user, post, null, body);
            redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully");
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        }

    }

    @PostMapping("/b/{boardName}/posts/{slug}/comments/{commentId}/reply")
    public String addChildComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @PathVariable UUID commentId,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUserDetails(userDetails);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Post post = postService.findByBoardAndSlug(board, slug);
        Comment parentComment = commentService.getComment(commentId);

        if (!parentComment.getPost().getId().equals(post.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment does not belong to this post");
        }

        try {
            commentService.addComment(user, post, parentComment, body);
            redirectAttributes.addFlashAttribute("successMessage", "Comment added successfully");
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        }
    }

    @PostMapping("/b/{boardName}/posts/{slug}/comments/{commentId}/edit")
    public String editComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @PathVariable UUID commentId,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUserDetails(userDetails);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Board board = boardService.getBoardAndCheckAccess(boardName, user);

        Comment comment = commentService.getComment(commentId);

        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            commentService.updateComment(user, comment, body);
            redirectAttributes.addFlashAttribute("successMessage", "Comment updated successfully");
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        }
    }

    @PostMapping("/b/{boardName}/posts/{slug}/comments/{commentId}/delete")
    public String deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @PathVariable UUID commentId,
            RedirectAttributes redirectAttributes
    ){
        User user = userService.findByUserDetails(userDetails);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Comment comment = commentService.getComment(commentId);

        if (comment.getUser() == null || !comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            commentService.deleteComment(user, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted successfully");
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + board.getName() + "/posts/" + slug;
        }
    }


}
