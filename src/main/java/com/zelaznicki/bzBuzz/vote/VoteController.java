package com.zelaznicki.bzBuzz.vote;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.comment.Comment;
import com.zelaznicki.bzBuzz.comment.CommentService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VoteController {

    private final UserService userService;
    private final BoardService boardService;
    private final PostService postService;
    private final CommentService commentService;


    private User requireAuthenticatedUser(UserDetails userDetails) {
        User user = userService.findByUserDetails(userDetails);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return user;
    }
    /**
     * Apply a vote by the authenticated user to the specified post and return the result as JSON.
     *
     * Resolves the authenticated principal into the application user, loads the post by slug,
     * performs the vote operation, and responds with the updated vote score and the action taken.
     *
     * @param userDetails the Spring Security principal for the currently authenticated user
     * @param boardName the board's name from the request path
     * @param slug the post's slug from the request path
     * @param voteType an integer representing the vote action
     * @return a JSON object with keys `voteScore` (the updated score) and `action` (the performed action) on success;
     *         on invalid input returns a JSON object with key `error` and a 400 Bad Request status
     */
    @PostMapping("/api/b/{boardName}/posts/{slug}/vote")
    public ResponseEntity<?> voteApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @RequestParam int voteType
    ) {
        User user = requireAuthenticatedUser(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Post post = postService.getPostAndCheckAccess(boardName, slug, user);
        VoteResponse result = postService.vote(post, user, voteType);
        return ResponseEntity.ok(Map.of("voteScore", result.voteScore(), "action", result.action()));

    }

    @PostMapping("/api/b/{boardName}/posts/{slug}/comments/{commentId}/vote")
    public ResponseEntity<?> voteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @PathVariable UUID commentId,
            @RequestParam int voteType
    ) {
        User user = requireAuthenticatedUser(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Comment comment = commentService.getComment(commentId);

        Post post;
        try {
            post = postService.findByBoardAndSlug(board, slug);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }

        if (!comment.getPost().getId().equals(post.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
            VoteResponse result = commentService.vote(comment, user, voteType);
            return ResponseEntity.ok(Map.of("voteScore", result.voteScore(), "action", result.action()));

    }
}
