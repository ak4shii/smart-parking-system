package com.smart_parking_system.backend.repository;

import com.smart_parking_system.backend.entity.Lcd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LcdRepository extends JpaRepository<Lcd, Integer> {
    
    java.util.Optional<Lcd> findByMcIdAndName(Integer mcId, String name);
}



