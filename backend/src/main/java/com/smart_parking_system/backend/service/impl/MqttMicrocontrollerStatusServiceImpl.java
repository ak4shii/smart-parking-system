package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.service.MqttMicrocontrollerStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MqttMicrocontrollerStatusServiceImpl implements MqttMicrocontrollerStatusService {

    private final MicrocontrollerRepository microcontrollerRepository;

    @Override
    @Transactional
    public void handleStatus(String mcCode, MqttStatusRequestDto status) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found: " + mcCode));

        if (status.getOnline() != null) {
            mc.setOnline(status.getOnline());
        } else {
            mc.setOnline(true);
        }

        if (status.getUptimeSec() != null) {
            mc.setUptimeSec(status.getUptimeSec());
        }

        mc.setLastSeen(Instant.now());

        microcontrollerRepository.save(mc);
        microcontrollerRepository.flush();
    }
}
