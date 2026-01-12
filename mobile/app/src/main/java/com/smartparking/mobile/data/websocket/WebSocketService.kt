package com.smartparking.mobile.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.smartparking.mobile.BuildConfig
import com.smartparking.mobile.data.local.TokenDataStore
import com.smartparking.mobile.data.model.EntryLog
import com.smartparking.mobile.data.model.Sensor
import com.smartparking.mobile.data.model.Slot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket events for real-time updates
 */
sealed class WebSocketEvent {
    data class SlotUpdate(val slot: Slot) : WebSocketEvent()
    data class SensorUpdate(val sensor: Sensor) : WebSocketEvent()
    data class EntryLogEvent(val entryLog: EntryLog) : WebSocketEvent()
    data class OverviewUpdate(val data: JsonObject) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}

/**
 * STOMP Frame for WebSocket communication with Spring Boot
 */
data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
) {
    fun serialize(): String {
        val sb = StringBuilder()
        sb.append(command).append("\n")
        headers.forEach { (key, value) ->
            sb.append("$key:$value\n")
        }
        sb.append("\n")
        sb.append(body)
        sb.append("\u0000")
        return sb.toString()
    }

    companion object {
        fun parse(data: String): StompFrame? {
            try {
                val lines = data.split("\n")
                if (lines.isEmpty()) return null

                val command = lines[0]
                val headers = mutableMapOf<String, String>()
                var bodyStartIndex = 1

                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.isEmpty()) {
                        bodyStartIndex = i + 1
                        break
                    }
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0) {
                        headers[line.substring(0, colonIndex)] = line.substring(colonIndex + 1)
                    }
                }

                val body = if (bodyStartIndex < lines.size) {
                    lines.subList(bodyStartIndex, lines.size).joinToString("\n").trimEnd('\u0000')
                } else ""

                return StompFrame(command, headers, body)
            } catch (e: Exception) {
                Log.e("StompFrame", "Error parsing frame", e)
                return null
            }
        }
    }
}

@Singleton
class WebSocketService @Inject constructor(
    private val tokenDataStore: TokenDataStore
) {
    private val TAG = "WebSocketService"
    private val gson = Gson()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 3000L
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var subscriptionId = 0
    
    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 100)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()
    
    private val subscriptions = mutableSetOf<String>()
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
        scope.launch {
            val token = tokenDataStore.token.first()
            if (token.isNullOrEmpty()) {
                Log.w(TAG, "No token available, skipping WebSocket connection")
                return@launch
            }
            
            if (isConnected) {
                Log.d(TAG, "Already connected")
                return@launch
            }
            
            connectInternal(token)
        }
    }
    
    private fun connectInternal(token: String) {
        val wsUrl = BuildConfig.WS_BASE_URL
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                isConnected = true
                reconnectAttempts = 0
                
                // Send STOMP CONNECT frame
                val connectFrame = StompFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "10000,10000",
                        "Authorization" to "Bearer $token"
                    )
                )
                webSocket.send(connectFrame.serialize())
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                handleMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                isConnected = false
                scope.launch {
                    _events.emit(WebSocketEvent.Error(t.message ?: "Connection failed"))
                }
                attemptReconnect(token)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                isConnected = false
                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
            }
        })
    }
    
    private fun handleMessage(text: String) {
        val frame = StompFrame.parse(text) ?: return
        
        when (frame.command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP connected")
                scope.launch {
                    _events.emit(WebSocketEvent.Connected)
                }
                // Re-subscribe to all topics
                resubscribeAll()
            }
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: return
                val body = frame.body
                
                try {
                    when {
                        destination.contains("overview_updates") -> {
                            val data = gson.fromJson(body, JsonObject::class.java)
                            scope.launch { _events.emit(WebSocketEvent.OverviewUpdate(data)) }
                        }
                        destination.contains("sensors") -> {
                            val sensor = gson.fromJson(body, Sensor::class.java)
                            scope.launch { _events.emit(WebSocketEvent.SensorUpdate(sensor)) }
                        }
                        destination.contains("entrylog") -> {
                            val entryLog = gson.fromJson(body, EntryLog::class.java)
                            scope.launch { _events.emit(WebSocketEvent.EntryLogEvent(entryLog)) }
                        }
                        destination.contains("slots") -> {
                            val slot = gson.fromJson(body, Slot::class.java)
                            scope.launch { _events.emit(WebSocketEvent.SlotUpdate(slot)) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}")
                }
            }
            "ERROR" -> {
                Log.e(TAG, "STOMP error: ${frame.body}")
                scope.launch {
                    _events.emit(WebSocketEvent.Error(frame.body))
                }
            }
        }
    }
    
    fun subscribe(topic: String) {
        subscriptions.add(topic)
        if (isConnected) {
            sendSubscribe(topic)
        }
    }
    
    private fun sendSubscribe(topic: String) {
        val id = "sub-${subscriptionId++}"
        val subscribeFrame = StompFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to id,
                "destination" to topic
            )
        )
        webSocket?.send(subscribeFrame.serialize())
        Log.d(TAG, "Subscribed to $topic")
    }
    
    private fun resubscribeAll() {
        // Default subscriptions for the app
        val defaultTopics = listOf(
            "/topic/overview_updates",
            "/topic/sensors",
            "/topic/entrylog_new_events"
        )
        
        (subscriptions + defaultTopics).forEach { topic ->
            sendSubscribe(topic)
        }
    }
    
    private fun attemptReconnect(token: String) {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            Log.d(TAG, "Reconnecting... attempt $reconnectAttempts/$maxReconnectAttempts")
            
            scope.launch {
                delay(reconnectDelayMs)
                connectInternal(token)
            }
        } else {
            Log.e(TAG, "Max reconnect attempts reached")
        }
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        subscriptions.clear()
    }
    
    fun isConnected(): Boolean = isConnected
}
