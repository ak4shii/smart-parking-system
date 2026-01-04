package com.smart_parking_system.backend.dto.mqtt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MqttEntryRequestDto {

    private String rfidCode;
}
