package com.bf.navigator.service.user.dto;

import com.bf.navigator.service.user.model.AccessibilityType;
import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<AccessibilityType> accessibilityTypes;
}
