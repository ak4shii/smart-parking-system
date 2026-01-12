package com.smartparking.mobile.data.model

data class ParkingSpace(
    val id: Long,
    val name: String,
    val location: String,
    val owner: String? = null
)

data class CreateParkingSpaceRequest(
    val name: String,
    val location: String
)

data class UpdateParkingSpaceRequest(
    val name: String,
    val location: String
)
