package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Lcd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LcdRepository extends JpaRepository<Lcd, Integer> {
    
    Optional<Lcd> findByMcIdAndName(Integer mcId, String name);

    boolean existsByName(String name);

    Optional<Lcd> findByName(String name);
}
