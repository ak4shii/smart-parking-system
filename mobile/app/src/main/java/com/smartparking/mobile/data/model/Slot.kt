package com.smartparking.mobile.data.model

data class Slot(
    val id: Long,
    val parkingSpaceId: Long,
    val isOccupied: Boolean
)
