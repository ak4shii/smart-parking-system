package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Rfid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RfidRepository extends JpaRepository<Rfid, Integer> {
    
    Optional<Rfid> findByRfidCode(String rfidCode);
}







