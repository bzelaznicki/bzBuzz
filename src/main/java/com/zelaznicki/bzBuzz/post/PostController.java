package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;


}
