package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.CreateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceDto;
import com.smart_parking_system.backend.dto.UpdateParkingSpaceRequestDto;
import com.smart_parking_system.backend.dto.ParkingSpaceManagerDto;
import com.smart_parking_system.backend.entity.ParkingSpace;
import com.smart_parking_system.backend.entity.User;
import com.smart_parking_system.backend.entity.UserParkingSpace;
import com.smart_parking_system.backend.entity.UserParkingSpaceId;
import com.smart_parking_system.backend.repository.ParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserParkingSpaceRepository;
import com.smart_parking_system.backend.repository.UserRepository;
import com.smart_parking_system.backend.service.IParkingSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingSpaceServiceImpl implements IParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserParkingSpaceRepository userParkingSpaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParkingSpaceDto createParkingSpace(CreateParkingSpaceRequestDto requestDto) {
        User currentUser = getCurrentUser();

        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setName(requestDto.getName());
        parkingSpace.setLocation(requestDto.getLocation());
        parkingSpace.setOwner(currentUser.getEmail());

        ParkingSpace savedParkingSpace = parkingSpaceRepository.save(parkingSpace);
        parkingSpaceRepository.flush();

        addMembership(currentUser, savedParkingSpace);

        return convertToDto(savedParkingSpace);
    }

    @Override
    public List<ParkingSpaceDto> getAllParkingSpaces() {
        User currentUser = getCurrentUser();
        List<Integer> psIds = userParkingSpaceRepository.findParkingSpaceIdsByUserId(currentUser.getId());
        if (psIds.isEmpty()) {
            return List.of();
        }
        return parkingSpaceRepository.findAllById(psIds)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParkingSpaceDto getParkingSpaceById(Integer id) {
        User currentUser = getCurrentUser();
        requireMembership(currentUser.getId(), id);

        ParkingSpace parkingSpace = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + id));
        return convertToDto(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpaceDto updateParkingSpace(Integer id, UpdateParkingSpaceRequestDto requestDto) {
        User currentUser = getCurrentUser();
        requireMembership(currentUser.getId(), id);

        ParkingSpace parkingSpace = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + id));

        parkingSpace.setName(requestDto.getName());
        parkingSpace.setLocation(requestDto.getLocation());

        ParkingSpace updatedParkingSpace = parkingSpaceRepository.save(parkingSpace);
        parkingSpaceRepository.flush();

        return convertToDto(updatedParkingSpace);
    }

    @Override
    @Transactional
    public void deleteParkingSpace(Integer id) {
        User currentUser = getCurrentUser();

        ParkingSpace parkingSpace = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + id));

        // Check if user is admin
        boolean isAdmin = isAdmin(currentUser);

        // Admins can delete any parking space, regular users must be members
        if (!isAdmin) {
            requireMembership(currentUser.getId(), id);
        }

        // Delete all user-parking space relationships
        List<Integer> userIds = userParkingSpaceRepository.findUserIdsByParkingSpaceId(id);
        for (Integer userId : userIds) {
            UserParkingSpaceId upsId = new UserParkingSpaceId();
            upsId.setUserId(userId);
            upsId.setPsId(id);
            userParkingSpaceRepository.deleteById(upsId);
        }

        // Delete the parking space itself
        parkingSpaceRepository.delete(parkingSpace);
    }

    @Override
    @Transactional
    public void addManager(Integer parkingSpaceId, String managerEmail) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(parkingSpaceId)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + parkingSpaceId));

        requireOwner(currentUser, ps);

        User otherUser = userRepository.findUserByEmail(managerEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + managerEmail));

        if (userParkingSpaceRepository.findByUserIdAndPsId(otherUser.getId(), parkingSpaceId).isPresent()) {
            throw new RuntimeException("User is already a manager of this parking space");
        }

        addMembership(otherUser, ps);
    }

    @Override
    @Transactional
    public void removeManager(Integer parkingSpaceId, Integer managerUserId) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(parkingSpaceId)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + parkingSpaceId));

        requireOwner(currentUser, ps);

        User ownerUser = userRepository.findUserByEmail(ps.getOwner())
                .orElse(null);
        if (ownerUser != null && ownerUser.getId().equals(managerUserId)) {
            throw new RuntimeException("Cannot remove the owner. Transfer ownership first.");
        }

        UserParkingSpaceId id = new UserParkingSpaceId();
        id.setUserId(managerUserId);
        id.setPsId(parkingSpaceId);

        if (userParkingSpaceRepository.findById(id).isEmpty()) {
            throw new RuntimeException("User is not a manager of this parking space");
        }

        List<Integer> userIds = userParkingSpaceRepository.findUserIdsByParkingSpaceId(parkingSpaceId);
        if (userIds.size() <= 1) {
            throw new RuntimeException("Cannot remove the last manager of a parking space");
        }

        userParkingSpaceRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void transferOwnership(Integer parkingSpaceId, Integer newOwnerUserId) {
        User currentUser = getCurrentUser();

        ParkingSpace ps = parkingSpaceRepository.findById(parkingSpaceId)
                .orElseThrow(() -> new RuntimeException("Parking space not found with id: " + parkingSpaceId));

        requireOwner(currentUser, ps);

        User newOwner = userRepository.findById(newOwnerUserId)
                .orElseThrow(() -> new RuntimeException("New owner user not found"));

        if (ps.getOwner() != null && ps.getOwner().equalsIgnoreCase(newOwner.getEmail())) {
            return;
        }

        if (userParkingSpaceRepository.findByUserIdAndPsId(newOwnerUserId, parkingSpaceId).isEmpty()) {
            throw new RuntimeException("New owner must already be a manager of this parking space");
        }

        ps.setOwner(newOwner.getEmail());
        parkingSpaceRepository.save(ps);
    }

    @Override
    public List<ParkingSpaceManagerDto> getManagers(Integer parkingSpaceId) {
        User currentUser = getCurrentUser();
        requireMembership(currentUser.getId(), parkingSpaceId);

        return userParkingSpaceRepository.findUsersByParkingSpaceId(parkingSpaceId)
                .stream()
                .map(u -> {
                    ParkingSpaceManagerDto dto = new ParkingSpaceManagerDto();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setEmail(u.getEmail());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void addMembership(User user, ParkingSpace ps) {
        UserParkingSpace ups = new UserParkingSpace();
        UserParkingSpaceId id = new UserParkingSpaceId();
        id.setUserId(user.getId());
        id.setPsId(ps.getId());
        ups.setId(id);
        ups.setUser(user);
        ups.setPs(ps);
        userParkingSpaceRepository.save(ups);
    }

    private void requireMembership(Integer userId, Integer parkingSpaceId) {
        if (userParkingSpaceRepository.findByUserIdAndPsId(userId, parkingSpaceId).isEmpty()) {
            throw new RuntimeException("Forbidden");
        }
    }

    private void requireOwner(User currentUser, ParkingSpace ps) {
        if (ps.getOwner() == null || !ps.getOwner().equalsIgnoreCase(currentUser.getEmail())) {
            throw new RuntimeException("Forbidden");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && user.getRole().equals("ROLE_ADMIN");
    }

    private ParkingSpaceDto convertToDto(ParkingSpace parkingSpace) {
        ParkingSpaceDto dto = new ParkingSpaceDto();
        BeanUtils.copyProperties(parkingSpace, dto);
        dto.setId(parkingSpace.getId());
        dto.setOwner(parkingSpace.getOwner());
        return dto;
    }
}
