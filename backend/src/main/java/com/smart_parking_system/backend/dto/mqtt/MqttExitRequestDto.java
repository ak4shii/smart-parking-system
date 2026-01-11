package com.smart_parking_system.backend.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MqttExitRequestDto {

    @JsonProperty("rfidCode")
    private String rfidCode;
}
