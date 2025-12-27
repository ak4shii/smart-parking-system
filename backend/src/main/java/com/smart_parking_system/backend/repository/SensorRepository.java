package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    
}


