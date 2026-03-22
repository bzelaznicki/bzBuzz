package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.board.BoardService;
import com.zelaznicki.bzBuzz.user.User;
import com.zelaznicki.bzBuzz.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final BoardService boardService;
    private final UserService userService;
    private final PostVoteRepository postVoteRepository;

    /**
     * Resolves the board that the post is in
     * @param boardName board's unique name
     * @param slug post slug
     * @param user the current authenticated user, or null for anonymous access
     * @return the post if it's validated
     */
    private Post getPostAndCheckAccess(String boardName, String slug, User user) {
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        return postService.findByBoardAndSlug(board, slug);
    }

    /**
     * Render the post view page for a post identified by `slug` within the specified board and populate the model.
     *
     * Populates the model with `userVotes`, `currentUser`, `board`, `post`, and `isMember`. Enforces access rules for private boards.
     *
     * @param userDetails the authenticated principal (may be null for anonymous requests)
     * @param boardName the name of the board containing the post
     * @param slug the post's slug identifier
     * @param model the MVC model to populate for the view
     * @return the view name for displaying a post ("post/view")
     * @throws org.springframework.web.server.ResponseStatusException if the board is not found (404) or access is forbidden for private boards (403)
     */
    @GetMapping("/b/{boardName}/posts/{slug}")
    public String postPage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String boardName, @PathVariable String slug, Model model) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        Post post = getPostAndCheckAccess(boardName, slug, user);

        if (board.isPrivate() && !isMember) {
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        model.addAttribute("userVotes", postService.findVoteByPostAndUser(post, user));
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("post", post);
        model.addAttribute("isMember", isMember);
        return "post/view";
    }

    /**
     * Prepare the model and enforce access for the board's "submit post" page.
     *
     * @param userDetails the authentication principal used to resolve the current application User; may be null
     * @param boardName   the name of the board for which the submit page is requested
     * @param model       the MVC model populated with `currentUser`, `board`, and `isMember`
     * @return            the view name for the post creation page ("post/create")
     */
    @GetMapping("/b/{boardName}/submit")
    public String submitPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            Model model
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("isMember", isMember);

        return "post/create";

    }

    /**
     * Render the edit page for a post in the specified board.
     *
     * The model is populated with `currentUser`, `board`, `post`, and `isMember`.
     *
     * @param boardName the name of the board containing the post
     * @param slug      the post's slug identifier
     * @param model     MVC model used to expose attributes to the view
     * @return          the view name for the post edit page ("post/edit")
     */
    @GetMapping("/b/{boardName}/posts/{slug}/edit")
    public String editPostPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            Model model
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        boolean isMember = user != null && boardService.isMember(board, user);
        Post post = getPostAndCheckAccess(board.getName(), slug, user);
        model.addAttribute("currentUser", user);
        model.addAttribute("board", board);
        model.addAttribute("post", post);
        model.addAttribute("isMember", isMember);

        return "post/edit";
    }


    /**
     * Creates a new post on the specified board and redirects to the created post on success,
     * or back to the board page on validation error.
     *
     * @param userDetails        the authenticated user's details (may be null for anonymous)
     * @param boardName          the board's name where the post will be created
     * @param title              the post title
     * @param text               optional post body text
     * @param postType           the type of the post (e.g., text, link)
     * @param url                optional URL for link posts
     * @param redirectAttributes used to set flash attributes for success or error messages
     * @return                   a redirect view: to the new post on success or to the board on error
     * @throws ResponseStatusException if the target board is private and the current user is not a member
     */
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
            return "redirect:/b/" + board.getName() + "/posts/" + post.getSlug();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName;
        }
    }

    /**
     * Handles a POST request to delete the post identified by `slug` and redirects to the appropriate page.
     *
     * On success, adds a flash attribute `successMessage = "Post deleted"` and redirects to `/b/{boardName}`.
     * If deletion fails with an `IllegalArgumentException`, adds a flash attribute `errorMessage` with the exception
     * message and redirects back to `/b/{boardName}/posts/{slug}`.
     *
     * @return the redirect view: on success `redirect:/b/{boardName}`, on failure `redirect:/b/{boardName}/posts/{slug}`
     */
    @PostMapping("/b/{boardName}/posts/{slug}/delete")
    public String deletePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Post post = getPostAndCheckAccess(boardName, slug, user);
        if (!post.getCreator().equals(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not the owner of the post");
            return "redirect:/b/" + boardName + "/posts/" + post.getSlug();
        }
        try {
            postService.deletePost(slug, user);

            redirectAttributes.addFlashAttribute("successMessage", "Post deleted");
            return "redirect:/b/" + boardName;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + board.getName() + "/posts/" + post.getSlug();
        }
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
    @ResponseBody
    public ResponseEntity<?> voteApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @PathVariable String slug,
            @RequestParam int voteType
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Post post = getPostAndCheckAccess(boardName, slug, user);

        boolean isMember = user != null && boardService.isMember(board, user);

        if (board.isPrivate() && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this board");
        }
        try {
            VoteResponse result = postService.vote(post, user, voteType);
            return ResponseEntity.ok(Map.of("voteScore", result.voteScore(), "action", result.action()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Handle form submission to update an existing post and redirect to the appropriate page.
     *
     * @param userDetails         the authenticated principal for the current user
     * @param boardName           the board's name used to build redirect URLs
     * @param title               the new title for the post
     * @param text                the new text content for the post (may be null)
     * @param url                 the new URL for the post (may be null)
     * @param slug                the slug identifying the post to update
     * @param redirectAttributes  used to add flash attributes; sets `successMessage` on success or `errorMessage` on failure
     * @return                    the view name for a redirect: on success redirects to the updated post page, on failure redirects back to the post edit page
     */
    @PostMapping("/b/{boardName}/posts/{slug}/edit")
    public String editPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String boardName,
            @RequestParam String title,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) String url,
            @PathVariable String slug,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByUserDetails(userDetails);
        Board board = boardService.getBoardAndCheckAccess(boardName, user);
        Post post = getPostAndCheckAccess(boardName, slug, user);

        if  (!post.getCreator().equals(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not the owner of the post");
            return "redirect:/b/" + boardName + "/posts/" + post.getSlug();
        }

        try {

            Post updatedPost = postService.updatePost(slug, user, title, text, postType, url);
            redirectAttributes.addFlashAttribute("successMessage", "Post updated");
            return "redirect:/b/" + board.getName() + "/posts/" + updatedPost.getSlug();

        }  catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/" + boardName + "/posts/" + slug;
        }
    }
}
