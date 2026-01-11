package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Microcontroller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MicrocontrollerRepository extends JpaRepository<Microcontroller, Integer> {

    Optional<Microcontroller> findByMcCode(String mcCode);

    Optional<Microcontroller> findByMqttUsername(String mqttUsername);

    boolean existsByMqttUsername(String mqttUsername);

    @Query("SELECT m FROM Microcontroller m WHERE m.online = true AND m.lastSeen < :threshold")
    List<Microcontroller> findOnlineWithLastSeenBefore(@Param("threshold") Instant threshold);
}
