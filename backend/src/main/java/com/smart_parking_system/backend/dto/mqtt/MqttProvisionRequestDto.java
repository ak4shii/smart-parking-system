package com.smart_parking_system.backend.dto.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttProvisionRequestDto {

    private List<ComponentDto> doors;
    private List<ComponentDto> lcds;
    private List<SensorComponentDto> sensors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentDto {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorComponentDto {
        private String name;
        private String type;
        private String slotName;
    }
}

