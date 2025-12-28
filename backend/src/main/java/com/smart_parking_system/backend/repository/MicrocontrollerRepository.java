package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Microcontroller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MicrocontrollerRepository extends JpaRepository<Microcontroller, Integer> {
    
    Optional<Microcontroller> findByMcCode(String mcCode);
}





