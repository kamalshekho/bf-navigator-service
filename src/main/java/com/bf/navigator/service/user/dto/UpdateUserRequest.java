package com.bf.navigator.service.user.dto;

import com.bf.navigator.service.user.model.AccessibilityType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private Set<AccessibilityType> accessibilityTypes;
}
