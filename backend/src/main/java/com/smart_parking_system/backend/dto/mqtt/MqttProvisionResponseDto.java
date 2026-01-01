package com.smart_parking_system.backend.dto.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttProvisionResponseDto {

    private boolean success;
    private String message;
    private List<ComponentResponseDto> doors;
    private List<ComponentResponseDto> lcds;
    private List<ComponentResponseDto> sensors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentResponseDto {
        private Integer id;
        private String name;
    }
}


