package com.smart_parking_system.backend.controller;

import com.smart_parking_system.backend.dto.AdminUserDto;
import com.smart_parking_system.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins/users")
@RequiredArgsConstructor
public class AdminController {

    private final IUserService userService;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        try {
            return ResponseEntity.ok(userService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable("id") Integer userId) {
        try {
            return ResponseEntity.ok(userService.getUserById(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable("id") Integer userId) {
        try {
            userService.setUserEnabled(userId, true);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable("id") Integer userId) {
        try {
            userService.setUserEnabled(userId, false);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
