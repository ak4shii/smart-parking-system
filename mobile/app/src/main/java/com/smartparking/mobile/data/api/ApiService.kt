package com.smartparking.mobile.data.api

import com.smartparking.mobile.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============== Auth ==============
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // ============== Parking Spaces ==============
    @GET("api/parking-spaces")
    suspend fun getAllParkingSpaces(): Response<List<ParkingSpace>>

    @POST("api/parking-spaces")
    suspend fun createParkingSpace(@Body request: CreateParkingSpaceRequest): Response<ParkingSpace>

    @PUT("api/parking-spaces/{id}")
    suspend fun updateParkingSpace(
        @Path("id") id: Long,
        @Body request: UpdateParkingSpaceRequest
    ): Response<ParkingSpace>

    @DELETE("api/parking-spaces/{id}")
    suspend fun deleteParkingSpace(@Path("id") id: Long): Response<Unit>

    // ============== Slots ==============
    @GET("api/slots")
    suspend fun getAllSlots(): Response<List<Slot>>

    @GET("api/slots/{id}")
    suspend fun getSlotById(@Path("id") id: Long): Response<Slot>

    // ============== Entry Logs ==============
    @GET("api/entry-logs")
    suspend fun getEntryLogsByParkingSpace(
        @Query("parkingSpaceId") parkingSpaceId: Long
    ): Response<List<EntryLog>>

    @GET("api/entry-logs/{id}")
    suspend fun getEntryLogById(@Path("id") id: Long): Response<EntryLog>

    // ============== Sensors ==============
    @GET("api/sensors")
    suspend fun getAllSensors(): Response<List<Sensor>>

    // ============== Microcontrollers ==============
    @GET("api/microcontrollers")
    suspend fun getAllMicrocontrollers(): Response<List<Microcontroller>>

    @GET("api/microcontrollers/{id}")
    suspend fun getMicrocontrollerById(@Path("id") id: Long): Response<Microcontroller>

    @POST("api/microcontrollers")
    suspend fun createMicrocontroller(@Body request: CreateMicrocontrollerRequest): Response<CreateMicrocontrollerResponse>

    @POST("api/microcontrollers/{id}/mqtt/regenerate")
    suspend fun regenerateMqttCredentials(@Path("id") id: Long): Response<MqttCredentials>

    @POST("api/microcontrollers/{id}/mqtt/revoke")
    suspend fun revokeMqttCredentials(@Path("id") id: Long): Response<Unit>

    // ============== S3 ==============
    @GET("api/s3/presign")
    suspend fun presignGetUrl(@Query("key") key: String): Response<String>
}
