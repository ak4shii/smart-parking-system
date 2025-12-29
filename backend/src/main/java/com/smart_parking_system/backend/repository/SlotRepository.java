package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotRepository extends JpaRepository<Slot, Integer> {
    
    java.util.Optional<Slot> findByName(String name);
}







