package com.smart.therapy.flow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicture;
    private Boolean active;
    private List<String> roles;
    private Instant lastLogin;
    private Instant createdAt;
    private Instant updatedAt;

    private UserProfileResponse profile;
}


