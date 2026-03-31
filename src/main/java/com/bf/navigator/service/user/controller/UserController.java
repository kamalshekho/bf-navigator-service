package com.bf.navigator.service.user.controller;

import com.bf.navigator.service.user.dto.UpdateUserRequest;
import com.bf.navigator.service.user.dto.UserDTO;
import com.bf.navigator.service.user.model.User;
import com.bf.navigator.service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(mapToDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateMe(@RequestBody UpdateUserRequest request, Authentication authentication) {
        String email = authentication.getName();

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getAccessibilityTypes() != null) {
            user.setAccessibilityTypes(request.getAccessibilityTypes());
        }

        User updatedUser = userService.save(user);

        return ResponseEntity.ok(mapToDto(updatedUser));
    }

    private UserDTO mapToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAccessibilityTypes(user.getAccessibilityTypes());
        return dto;
    }
}
