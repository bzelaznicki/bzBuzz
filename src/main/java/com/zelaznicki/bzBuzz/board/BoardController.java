package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.common.PostSort;
import com.zelaznicki.bzBuzz.post.Post;
import com.zelaznicki.bzBuzz.post.PostService;
import com.zelaznicki.bzBuzz.user.User;
import com.zelaznicki.bzBuzz.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final UserService userService;
    private final PostService postService;

    /**
     * Render the home page and populate the model with the current user and public boards.
     *
     * @param userDetails the authenticated user's details from Spring Security, or {@code null} if unauthenticated
     * @param model       the Spring MVC model to populate for the view
     * @return            the view name "home"
     */
    @GetMapping("/")
    public String homepage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userService.findByUserDetails(userDetails));
        model.addAttribute("boards", boardService.findPublicBoards());
        return "home";
    }

    /**
     * Render the board creation page and expose the currently authenticated user to the view.
     *
     * @param userDetails the Spring Security principal for the current request (may be null for anonymous users)
     * @param model       the MVC model to populate attributes for the view; this method adds a `currentUser` attribute
     * @return            the view name for the board creation page ("board/create")
     */
    @GetMapping("/b/create")
    public String createBoard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUser",  userService.findByUserDetails(userDetails));
        return "board/create";
    }

    /**
     * Render a board page showing board details, posts, membership status, and the current user's votes.
     *
     * Adds the following model attributes: "currentUser", "board", "isMember", "posts", and "userVotes".
     *
     * @param userDetails the authenticated principal (may be null)
     * @param name the board name from the path
     * @param model the MVC model to populate for the view
     * @return the view name "board/home"
     * @throws ResponseStatusException with status 404 if no board with the given name exists
     * @throws ResponseStatusException with status 403 if the board is private and the user is not a member
     */
    @GetMapping("/b/{name}")
    public String boardPage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String name, Model model) {
        User user =  userService.findByUserDetails(userDetails);
        model.addAttribute("currentUser", user);
        Board board;

        try {
            board = boardService.findByName(name);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        boolean isMember = user != null && boardService.isMember(board, user);
        if (board.isPrivate() && !isMember) {
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN);
        }



        model.addAttribute("board", board);

        model.addAttribute("isMember", isMember);

        List<Post> posts = postService.findByBoard(board, PostSort.TOP);

        model.addAttribute("posts", posts);

        if (user != null) {
            Map<UUID, Integer> voteMap = postService.findVotesByBoardAndUser(board, user);
            model.addAttribute("userVotes", voteMap);
        } else {
            model.addAttribute("userVotes", Map.of());
        }


        return "board/home";
    }

    /**
     * Creates a new board with the given properties and redirects to the newly created board page.
     *
     * @param userDetails      the currently authenticated user's details
     * @param name             the desired unique name for the board
     * @param description      optional textual description for the board; may be null
     * @param bannerUrl        optional URL of the board banner image; may be null
     * @param isPrivate        `true` to create a private board, `false` to create a public board
     * @param redirectAttributes flash attributes used to convey success or error messages to the redirected view
     * @return                 a redirect string to the created board on success, or a redirect back to the board creation page on error
     */
    @PostMapping("/b/create")
    public String create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam(required = false)String description,
            @RequestParam(required = false) String bannerUrl,
            @RequestParam(defaultValue = "false") boolean isPrivate,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User user =  userService.findByUserDetails(userDetails);

            Board board = boardService.create(name, description, bannerUrl, user, isPrivate);


            redirectAttributes.addFlashAttribute("successMessage", "Board created");
            return "redirect:/b/" + board.getName();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/create";
        }
    }

    /**
     * Join the authenticated user to the board identified by `name` and redirect back to that board page.
     *
     * On success a flash attribute "successMessage" is added; on failure a flash attribute "errorMessage"
     * containing the exception message is added.
     *
     * @param userDetails         the Spring Security principal for the currently authenticated user
     * @param name                the board's name (path variable)
     * @param redirectAttributes  used to add flash attributes for the redirect
     * @return                    the redirect view name to the board page (e.g. "redirect:/b/{name}")
     */
    @PostMapping("/b/{name}/join")
    public String joinBoard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            RedirectAttributes redirectAttributes
    ) {

        try {
            User user = userService.findByUserDetails(userDetails);
            Board board = boardService.findByName(name);
            boardService.joinBoard(board, user);

            redirectAttributes.addFlashAttribute("successMessage", "Board joined");

        } catch  (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/b/" + name;

    }

    /**
     * Removes the authenticated user from the board identified by {@code name} and redirects back to that board's page.
     *
     * If the removal succeeds, adds a flash attribute named {@code successMessage} with value {@code "Left board"}.
     * If an {@link IllegalArgumentException} occurs, adds a flash attribute named {@code errorMessage} containing the exception message.
     *
     * @param userDetails        the Spring Security principal for the currently authenticated user
     * @param name               the unique name of the board to leave
     * @param redirectAttributes container for flash attributes to be available after the redirect
     * @return                   a redirect string to the board page ("/b/{name}")
     */
    @PostMapping("/b/{name}/leave")
    public String leaveBoard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            RedirectAttributes redirectAttributes
    ) {

        try {
            User user = userService.findByUserDetails(userDetails);
            Board board = boardService.findByName(name);
            boardService.removeMemberFromBoard(board, user);

            redirectAttributes.addFlashAttribute("successMessage", "Left board");
        } catch  (IllegalArgumentException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/b/" + name;

    }

}
