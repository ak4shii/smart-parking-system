package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.AdminUserDto;
import com.smart_parking_system.backend.dto.UserDto;
import com.smart_parking_system.backend.dto.UserSearchDto;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void setUserEnabled(Integer userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(enabled);
        userRepository.save(user);
        userRepository.flush();
    }

    @Override
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdminUserDto getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return toAdminDto(user);
    }

    @Override
    public UserDto getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);
        dto.setId(user.getId());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());

        return dto;
    }

    @Override
    public List<UserSearchDto> searchUsers(String q) {
        if (q == null || q.trim().length() < 3) {
            throw new RuntimeException("Query must be at least 3 characters");
        }
        String query = q.trim();

        return userRepository.searchEnabledUsers(query, PageRequest.of(0, 10)).stream()
                .map(this::toSearchDto)
                .collect(Collectors.toList());
    }

    private AdminUserDto toAdminDto(User user) {
        AdminUserDto dto = new AdminUserDto();
        BeanUtils.copyProperties(user, dto);
        dto.setId(user.getId());
        return dto;
    }

    private UserSearchDto toSearchDto(User user) {
        UserSearchDto dto = new UserSearchDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }
}
