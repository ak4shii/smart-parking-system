package com.smart_parking_system.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "microcontroller")
public class Microcontroller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mc_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "mc_code", nullable = false, unique = true)
    private String mcCode;

    @Column(name = "name")
    private String name;

    @ColumnDefault("false")
    @Column(name = "online")
    private Boolean online;

    @Column(name = "uptime_sec")
    private Long uptimeSec;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ps_id", nullable = false)
    private ParkingSpace ps;

    // MQTT Security Fields
    @Column(name = "mqtt_username", unique = true)
    private String mqttUsername;

    @Column(name = "mqtt_password_hash")
    private String mqttPasswordHash;

    @ColumnDefault("true")
    @Column(name = "mqtt_enabled")
    private Boolean mqttEnabled;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (mqttEnabled == null) {
            mqttEnabled = true;
        }
    }
}
