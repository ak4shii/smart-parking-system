package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.MicrocontrollerDto;
import com.smart_parking_system.backend.dto.mqtt.MqttStatusRequestDto;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.service.IMqttMicrocontrollerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MqttMicrocontrollerServiceImpl implements IMqttMicrocontrollerService {

    private final MicrocontrollerRepository microcontrollerRepository;

    @Override
    @Transactional
    public MicrocontrollerDto handleStatus(String mcCode, MqttStatusRequestDto status) {
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

        Microcontroller saved = microcontrollerRepository.save(mc);
        microcontrollerRepository.flush();

        return toDto(saved);
    }

    private MicrocontrollerDto toDto(Microcontroller mc) {
        MicrocontrollerDto dto = new MicrocontrollerDto();
        BeanUtils.copyProperties(mc, dto);
        dto.setId(mc.getId());
        if (mc.getPs() != null) {
            dto.setParkingSpaceId(mc.getPs().getId());
        }
        return dto;
    }
}
