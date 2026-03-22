package com.zelaznicki.bzBuzz.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with the given username, email, and raw password, then persists and returns the saved user.
     *
     * @param username the desired username for the new user
     * @param email the email address for the new user
     * @param password the raw (unhashed) password to be encoded and stored
     * @return the persisted User entity
     * @throws IllegalArgumentException if the email is already in use or the username is already taken
     */
    public User register(String username, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .build();

        return userRepository.save(user);

    }

    /**
     * Resolve a User from a Spring Security UserDetails by using the UserDetails' username as the email lookup key.
     *
     * @param userDetails the UserDetails whose username will be used as the email to look up the User; may be null
     * @return null if userDetails is null, otherwise the matching User
     * @throws IllegalArgumentException if no User exists for the username (used as email)
     */
    public User findByUserDetails(UserDetails userDetails) {
        if (userDetails == null) return null;
        return findByEmail(userDetails.getUsername());
    }

    /**
     * Finds a user by email.
     *
     * @param email the email address used to locate the user
     * @return the User with the specified email
     * @throws IllegalArgumentException if no user with the given email exists
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new  IllegalArgumentException("User not found"));
    }
}

