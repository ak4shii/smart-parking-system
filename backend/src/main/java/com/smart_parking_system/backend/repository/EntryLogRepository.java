package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.EntryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntryLogRepository extends JpaRepository<EntryLog, Integer> {

    @Query("SELECT el FROM EntryLog el WHERE el.rfid.id = :rfidId AND el.outTime IS NULL")
    Optional<EntryLog> findActiveByRfidId(@Param("rfidId") Integer rfidId);

    @Query("SELECT el FROM EntryLog el WHERE el.rfid.ps.id = :psId ORDER BY el.inTime DESC")
    List<EntryLog> findAllByParkingSpaceId(@Param("psId") Integer psId);
}







