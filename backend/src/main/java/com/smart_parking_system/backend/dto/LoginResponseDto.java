package com.smart_parking_system.backend.dto;

public record LoginResponseDto(String message, UserDto user, String jwtToken) {

}