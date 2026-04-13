package com.zelaznicki.bzBuzz.user;

import com.zelaznicki.bzBuzz.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private UserDetails userDetails;

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
        when(userRepository.existsByEmail("newuser@example.com"))
            .thenReturn(false);
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

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        User created = userService.register(user.getUsername(), user.getEmail(), "strongPassWord1!");

        verify(passwordEncoder).encode("strongPassWord1!");
        verify(userRepository).save(argThat(
                u -> u.getUsername().equals(user.getUsername())
            && u.getPasswordHash().equals("$2a$10$hashedpassword")
                && u.getEmail().equals(user.getEmail())
                ));
        assertThat(created.getUsername()).isEqualTo(user.getUsername());
        assertThat(created.getPasswordHash()).isEqualTo("$2a$10$hashedpassword");
        assertThat(created.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void user_shouldThrowException_whenUserEmailDoesNotExist() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByEmail(user.getEmail())
        );

        assertThat(ex).hasMessage("User not found");
    }

    @Test
    void user_shouldThrowException_whenUsernameDoesNotExist() {
        when(userRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByUsername(user.getUsername())
        );

        assertThat(ex).hasMessage("User not found");
    }

    @Test
    void user_shouldReturnUser_whenUserIsFoundByEmail() {
        when(userRepository.findByEmail(user.getEmail()))
        .thenReturn(Optional.of(user));

        User foundUser = userService.findByEmail(user.getEmail());

        assertThat(foundUser).isEqualTo(user);
    }

    @Test
    void user_shouldReturnUser_whenUserIsFoundByUsername() {
        when(userRepository.findByUsername(user.getUsername()))
        .thenReturn(Optional.of(user));

        User foundUser = userService.findByUsername(user.getUsername());

        assertThat(foundUser).isEqualTo(user);
    }

    @Test
    void user_shouldReturnNull_whenUserDetailsIsNull() {
        User foundUser = userService.findByUserDetails(null);

        assertNull(foundUser);
        verifyNoInteractions(userRepository);
    }


    @Test
    void user_shouldReturnUser_whenUserDetailsIsValid() {
        when(userDetails.getUsername())
                .thenReturn(user.getEmail());

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        User result = userService.findByUserDetails(userDetails);
        assertThat(result).isEqualTo(user);
    }
}
