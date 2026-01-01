package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Door;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoorRepository extends JpaRepository<Door, Integer> {
    
    Optional<Door> findByMcIdAndName(Integer mcId, String name);
    
    boolean existsByName(String name);
    
    Optional<Door> findByName(String name);
}