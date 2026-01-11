package com.smart_parking_system.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class UserDto {
    
    private Integer id;
    private String username;
    private String email;
    private String role;
    private Instant createdAt;
}
