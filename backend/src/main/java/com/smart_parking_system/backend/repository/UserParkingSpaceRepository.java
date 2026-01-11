package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.UserParkingSpace;
import com.smart_parking_system.backend.entity.UserParkingSpaceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserParkingSpaceRepository extends JpaRepository<UserParkingSpace, UserParkingSpaceId> {

    @Query("SELECT ups.ps.id FROM UserParkingSpace ups WHERE ups.user.id = :userId")
    List<Integer> findParkingSpaceIdsByUserId(@Param("userId") Integer userId);

    @Query("SELECT ups FROM UserParkingSpace ups WHERE ups.user.id = :userId AND ups.ps.id = :psId")
    Optional<UserParkingSpace> findByUserIdAndPsId(@Param("userId") Integer userId, @Param("psId") Integer psId);

    @Query("SELECT ups.user.id FROM UserParkingSpace ups WHERE ups.ps.id = :psId")
    List<Integer> findUserIdsByParkingSpaceId(@Param("psId") Integer psId);

    @Query("SELECT ups.user FROM UserParkingSpace ups WHERE ups.ps.id = :psId")
    List<com.smart_parking_system.backend.entity.User> findUsersByParkingSpaceId(@Param("psId") Integer psId);

    void deleteByUserIdAndPsId(Integer userId, Integer psId);
}
