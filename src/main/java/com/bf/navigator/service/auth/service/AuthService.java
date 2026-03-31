package com.bf.navigator.service.auth.service;

import com.bf.navigator.service.auth.dto.AuthResponse;
import com.bf.navigator.service.auth.dto.LoginRequest;
import com.bf.navigator.service.auth.security.JwtService;
import com.bf.navigator.service.user.dto.RegisterRequest;
import com.bf.navigator.service.user.dto.UserDTO;
import com.bf.navigator.service.user.model.User;
import com.bf.navigator.service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserDTO register(RegisterRequest request) {
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already taken");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccessibilityTypes(
                request.getAccessibilityTypes() != null ? request.getAccessibilityTypes() : Set.of());

        User savedUser = userService.save(user);
        return userToDto(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String jwtToken = jwtService.generateToken(user.getEmail(), user.getId());
        AuthResponse response = new AuthResponse();
        response.setToken(jwtToken);
        return response;
    }

    private UserDTO userToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAccessibilityTypes(user.getAccessibilityTypes());
        return dto;
    }
}
