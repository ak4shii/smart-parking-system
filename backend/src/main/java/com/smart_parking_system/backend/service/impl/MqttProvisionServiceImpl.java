package com.smart_parking_system.backend.service.impl;

import com.smart_parking_system.backend.dto.mqtt.MqttProvisionRequestDto;
import com.smart_parking_system.backend.dto.mqtt.MqttProvisionResponseDto;
import com.smart_parking_system.backend.entity.Door;
import com.smart_parking_system.backend.entity.Lcd;
import com.smart_parking_system.backend.entity.Microcontroller;
import com.smart_parking_system.backend.entity.Sensor;
import com.smart_parking_system.backend.entity.Slot;
import com.smart_parking_system.backend.repository.DoorRepository;
import com.smart_parking_system.backend.repository.LcdRepository;
import com.smart_parking_system.backend.repository.MicrocontrollerRepository;
import com.smart_parking_system.backend.repository.SensorRepository;
import com.smart_parking_system.backend.repository.SlotRepository;
import com.smart_parking_system.backend.service.IMqttProvisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MqttProvisionServiceImpl implements IMqttProvisionService {

    private final MicrocontrollerRepository microcontrollerRepository;
    private final DoorRepository doorRepository;
    private final LcdRepository lcdRepository;
    private final SensorRepository sensorRepository;
    private final SlotRepository slotRepository;

    @Override
    @Transactional
    public MqttProvisionResponseDto handleProvision(String mcCode, MqttProvisionRequestDto request) {
        Microcontroller mc = microcontrollerRepository.findByMcCode(mcCode)
                .orElseThrow(() -> new RuntimeException("Microcontroller not found with mcCode: " + mcCode));

        List<MqttProvisionResponseDto.ComponentResponseDto> doorResponses = new ArrayList<>();
        List<MqttProvisionResponseDto.ComponentResponseDto> lcdResponses = new ArrayList<>();
        List<MqttProvisionResponseDto.ComponentResponseDto> sensorResponses = new ArrayList<>();

        if (request.getDoors() != null) {
            for (MqttProvisionRequestDto.ComponentDto doorDto : request.getDoors()) {
                Door door = findOrCreateDoor(mc, doorDto.getName());
                doorRepository.save(door);
                doorResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(door.getId(), door.getName()));
            }
        }

        if (request.getLcds() != null) {
            for (MqttProvisionRequestDto.ComponentDto lcdDto : request.getLcds()) {
                Lcd lcd = findOrCreateLcd(mc, lcdDto.getName());
                lcdRepository.save(lcd);
                lcdResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(lcd.getId(), lcd.getName()));
            }
        }

        if (request.getSensors() != null) {
            for (MqttProvisionRequestDto.SensorComponentDto sensorDto : request.getSensors()) {
                if (sensorDto.getSlotName() == null || sensorDto.getSlotName().trim().isEmpty()) {
                    continue;
                }
                
                Slot slot = findOrCreateSlot(mc, sensorDto.getSlotName());
                if (slot == null) {
                    continue;
                }
                
                Sensor sensor = findOrCreateSensor(mc, sensorDto, slot);
                sensorRepository.save(sensor);
                sensorResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(sensor.getId(), sensor.getName()));
            }
        }

        doorRepository.flush();
        lcdRepository.flush();
        sensorRepository.flush();

        return new MqttProvisionResponseDto(
                true,
                "Provisioning completed successfully",
                doorResponses,
                lcdResponses,
                sensorResponses
        );
    }

    private Door findOrCreateDoor(Microcontroller mc, String name) {
        return doorRepository.findByMcIdAndName(mc.getId(), name)
                .orElseGet(() -> {
                    Door door = new Door();
                    door.setName(name);
                    door.setMc(mc);
                    door.setIsOpened(false);
                    return door;
                });
    }

    private Lcd findOrCreateLcd(Microcontroller mc, String name) {
        return lcdRepository.findByMcIdAndName(mc.getId(), name)
                .orElseGet(() -> {
                    Lcd lcd = new Lcd();
                    lcd.setName(name);
                    lcd.setMc(mc);
                    lcd.setDisplayText("");
                    return lcd;
                });
    }

    private Slot findOrCreateSlot(Microcontroller mc, String slotName) {
        if (mc.getPs() == null) {
            return null;
        }
        
        return slotRepository.findByName(slotName)
                .orElseGet(() -> {
                    Slot slot = new Slot();
                    slot.setName(slotName);
                    slot.setPs(mc.getPs());
                    slot.setIsOccupied(false);
                    slotRepository.save(slot);
                    return slot;
                });
    }

    private Sensor findOrCreateSensor(Microcontroller mc, MqttProvisionRequestDto.SensorComponentDto sensorDto, Slot slot) {
        return sensorRepository.findByMcIdAndSlotIdAndName(mc.getId(), slot.getId(), sensorDto.getName())
                .orElseGet(() -> {
                    Sensor sensor = new Sensor();
                    sensor.setName(sensorDto.getName());
                    sensor.setType(sensorDto.getType());
                    sensor.setMc(mc);
                    sensor.setSlot(slot);
                    return sensor;
                });
    }
}

