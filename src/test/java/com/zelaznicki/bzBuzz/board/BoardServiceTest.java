package com.zelaznicki.bzBuzz.board;

import com.zelaznicki.bzBuzz.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    private User user;
    private Board board;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        board = Board.builder()
                .id(UUID.randomUUID())
                .createdBy(user)
                .name("test")
                .build();
    }
}
