package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.SensorDto;
import com.smart_parking_system.backend.dto.mqtt.MqttSensorStatusDto;
import com.smart_parking_system.backend.entity.Sensor;
import com.smart_parking_system.backend.entity.Slot;
import com.smart_parking_system.backend.repository.SensorRepository;
import com.smart_parking_system.backend.repository.SlotRepository;
import com.smart_parking_system.backend.service.IMqttSensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MqttSensorServiceImpl implements IMqttSensorService {

    private final SensorRepository sensorRepository;
    private final SlotRepository slotRepository;

    @Override
    @Transactional
    public SensorDto handleSensorStatus(String mcCode, MqttSensorStatusDto status) {
        Sensor sensor = sensorRepository.findById(status.getSensorId())
                .orElseThrow(() -> new RuntimeException("Sensor not found with id: " + status.getSensorId()));

        // Update slot occupancy based on sensor detection
        if (status.getIsOccupied() != null && sensor.getSlot() != null) {
            Slot slot = sensor.getSlot();
            slot.setIsOccupied(status.getIsOccupied());
            slotRepository.save(slot);
        }

        Sensor saved = sensorRepository.save(sensor);
        sensorRepository.flush();
        slotRepository.flush();

        return toDto(saved);
    }

    private SensorDto toDto(Sensor sensor) {
        SensorDto dto = new SensorDto();
        dto.setId(sensor.getId());
        dto.setName(sensor.getName());
        dto.setType(sensor.getType());
        if (sensor.getSlot() != null) {
            dto.setSlotId(sensor.getSlot().getId());
            dto.setSlotName(sensor.getSlot().getName());
            if (sensor.getSlot().getPs() != null) {
                dto.setParkingSpaceId(sensor.getSlot().getPs().getId());
            }
        }
        if (sensor.getMc() != null) {
            dto.setMicrocontrollerId(sensor.getMc().getId());
        }
        return dto;
    }
}

