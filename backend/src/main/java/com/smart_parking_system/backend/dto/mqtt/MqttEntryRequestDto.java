package com.smart_parking_system.backend.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MqttEntryRequestDto {

    @JsonProperty("rfidCode")
    private String rfidCode;
}
