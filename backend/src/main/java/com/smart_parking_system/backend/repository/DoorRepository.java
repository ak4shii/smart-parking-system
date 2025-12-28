package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Door;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoorRepository extends JpaRepository<Door, Integer> {
    
}



