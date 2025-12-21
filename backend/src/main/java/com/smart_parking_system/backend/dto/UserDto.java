package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private Boolean enabled;
    private Instant createdAt;
}
