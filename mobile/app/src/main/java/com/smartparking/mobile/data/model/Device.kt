package com.smartparking.mobile.data.model

data class Sensor(
    val id: Long,
    val name: String,
    val type: String, // "ultrasonic" or "infrared"
    val slotId: Long,
    val microcontrollerId: Long,
    val parkingSpaceId: Long
)

data class Microcontroller(
    val id: Long,
    val mcCode: String,
    val name: String,
    val online: Boolean,
    val uptimeSec: Long,
    val lastSeen: String?,
    val parkingSpaceId: Long,
    val mqttUsername: String?,
    val mqttEnabled: Boolean?
)

data class MqttCredentials(
    val mqttHost: String,
    val mqttPort: Int,
    val mqttUsername: String,
    val mqttPassword: String,
    val baseTopic: String,
    val mcCode: String,
    val deviceName: String
)

data class CreateMicrocontrollerRequest(
    val mcCode: String,
    val name: String,
    val parkingSpaceId: Long
)

data class CreateMicrocontrollerResponse(
    val id: Long,
    val mcCode: String,
    val name: String,
    val parkingSpaceId: Long,
    val mqttCredentials: MqttCredentials?
)
