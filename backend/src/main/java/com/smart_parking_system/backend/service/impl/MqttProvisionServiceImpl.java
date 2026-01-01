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
                doorRepository.saveAndFlush(door);
                doorResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(door.getId(), door.getName()));
            }
        }

        if (request.getLcds() != null) {
            for (MqttProvisionRequestDto.ComponentDto lcdDto : request.getLcds()) {
                Lcd lcd = findOrCreateLcd(mc, lcdDto.getName());
                lcdRepository.save(lcd);
                lcdRepository.saveAndFlush(lcd);
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
                sensorRepository.saveAndFlush(sensor);
                sensorResponses.add(new MqttProvisionResponseDto.ComponentResponseDto(sensor.getId(), sensor.getName()));
            }
        }


        return new MqttProvisionResponseDto(
                true,
                "Provisioning completed successfully",
                doorResponses,
                lcdResponses,
                sensorResponses
        );
    }

    private Door findOrCreateDoor(Microcontroller mc, String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("Door name is required");
        }

        return doorRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException("Door name already exists and is assigned to another microcontroller: " + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Door door = new Door();
                    door.setName(normalizedName);
                    door.setMc(mc);
                    door.setIsOpened(false);
                    return door;
                });
    }

    private Lcd findOrCreateLcd(Microcontroller mc, String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("LCD name is required");
        }

        return lcdRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException("LCD name already exists and is assigned to another microcontroller: " + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Lcd lcd = new Lcd();
                    lcd.setName(normalizedName);
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
        String normalizedName = sensorDto.getName() == null ? null : sensorDto.getName().trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("Sensor name is required");
        }

        return sensorRepository.findByName(normalizedName)
                .map(existing -> {
                    if (existing.getMc() != null && !existing.getMc().getId().equals(mc.getId())) {
                        throw new RuntimeException("Sensor name already exists and is assigned to another microcontroller: " + normalizedName);
                    }
                    if (existing.getSlot() != null && !existing.getSlot().getId().equals(slot.getId())) {
                        throw new RuntimeException("Sensor name already exists and is assigned to another slot: " + normalizedName);
                    }
                    if (existing.getMc() == null) {
                        existing.setMc(mc);
                    }
                    if (existing.getSlot() == null) {
                        existing.setSlot(slot);
                    }
                    if (sensorDto.getType() != null) {
                        existing.setType(sensorDto.getType());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Sensor sensor = new Sensor();
                    sensor.setName(normalizedName);
                    sensor.setType(sensorDto.getType());
                    sensor.setMc(mc);
                    sensor.setSlot(slot);
                    return sensor;
                });
    }
}

