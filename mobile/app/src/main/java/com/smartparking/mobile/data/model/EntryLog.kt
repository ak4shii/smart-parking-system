package com.smartparking.mobile.data.model

data class EntryLog(
    val id: Long,
    val rfidId: Long,
    val rfidCode: String,
    val licensePlate: String,
    val licensePlateImageKey: String?,
    val inTime: String,
    val outTime: String?,
    val parkingSpaceId: Long
)
