package com.smart_parking_system.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MicrocontrollerDto {

    private Integer id;
    private String mcCode;
    private String name;
    private Boolean online;
    private Long uptimeSec;
    private Instant lastSeen;
    private Integer parkingSpaceId;
    
    // MQTT Security fields
    private String mqttUsername;
    private Boolean mqttEnabled;
    
    // Only included when creating new device (one-time display)
    private MqttCredentialsResponseDto mqttCredentials;
}
