package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {

    Optional<Sensor> findByMcIdAndSlotIdAndName(Integer mcId, Integer slotId, String name);

    boolean existsByName(String name);

    Optional<Sensor> findByName(String name);
}
