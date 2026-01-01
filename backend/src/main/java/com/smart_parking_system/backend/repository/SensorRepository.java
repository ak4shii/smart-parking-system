package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    
    java.util.Optional<Sensor> findByMcIdAndSlotIdAndName(Integer mcId, Integer slotId, String name);
}







