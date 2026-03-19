package com.zelaznicki.bzBuzz.board;

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
public class BoardController {

    private final BoardService boardService;
    private final UserService userService;

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userService.findByEmail(userDetails.getUsername());
    }

    @GetMapping("/")
    public String homepage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUser", getCurrentUser(userDetails));
        model.addAttribute("boards", boardService.findPublicBoards());
        return "home";
    }

    @GetMapping("/b/create")
    public String createBoard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUser", getCurrentUser(userDetails));
        return "board/create";
    }

    @GetMapping("/b/{name}")
    public String boardPage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String name, Model model) {
        User user = getCurrentUser(userDetails);
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


        return "board/home";
    }

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

            User user = getCurrentUser(userDetails);

            Board board = boardService.create(name, description, bannerUrl, user, isPrivate);


            redirectAttributes.addFlashAttribute("successMessage", "Board created");
            return "redirect:/b/" + board.getName();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/b/create";
        }
    }

    @PostMapping("/b/{name}/join")
    public String joinBoard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            RedirectAttributes redirectAttributes
    ) {

        try {
            User user = getCurrentUser(userDetails);
            Board board = boardService.findByName(name);
            boardService.joinBoard(board, user);

            redirectAttributes.addFlashAttribute("successMessage", "Board joined");

        } catch  (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/b/" + name;

    }

    @PostMapping("/b/{name}/leave")
    public String leaveBoard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String name,
            RedirectAttributes redirectAttributes
    ) {

        try {
            User user = getCurrentUser(userDetails);
            Board board = boardService.findByName(name);
            boardService.removeMemberFromBoard(board, user);

            redirectAttributes.addFlashAttribute("successMessage", "Left board");
        } catch  (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/b/" + name;

    }

}
