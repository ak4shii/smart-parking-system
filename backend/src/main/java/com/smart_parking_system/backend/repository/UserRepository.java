package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findUserByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.enabled = true AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY u.username ASC")
    List<User> searchEnabledUsers(@Param("q") String q, Pageable pageable);
}
