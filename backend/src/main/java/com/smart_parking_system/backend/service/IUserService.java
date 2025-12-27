package com.smart_parking_system.backend.service;

import com.smart_parking_system.backend.dto.AdminUserDto;
import com.smart_parking_system.backend.dto.UserDto;
import com.smart_parking_system.backend.dto.UserSearchDto;

import java.util.List;

public interface IUserService {

    void setUserEnabled(Integer userId, boolean enabled);

    List<AdminUserDto> getAllUsers();

    AdminUserDto getUserById(Integer userId);

    UserDto getCurrentUserProfile();

    List<UserSearchDto> searchUsers(String q);
}
