package com.zelaznicki.bzBuzz.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;


    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .email("user@example.com")
                .build();
    }

    @Test
    void user_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("newUser", user.getEmail(), "strongPassWord1!")
        );

        assertThat(ex).hasMessage("Email already in use");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void user_shouldThrowException_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(user.getUsername(), "newuser@example.com", "strongPassWord1!")
        );

        assertThat(ex).hasMessage("Username already taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void user_shouldCreateNewUserAndHashPassword_whenUserDetailsAreCorrect() {
        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(false);

        when(passwordEncoder.encode("strongPassWord1!"))
                .thenReturn("$2a$10$hashedpassword");


        userService.register(user.getUsername(), user.getEmail(), "strongPassWord1!");

        verify(userRepository).save(argThat(
                u -> u.getUsername().equals(user.getUsername())
            && !u.getPasswordHash().equals("strongPassWord1!")
                && u.getEmail().equals(user.getEmail())
                ));
    }
}
