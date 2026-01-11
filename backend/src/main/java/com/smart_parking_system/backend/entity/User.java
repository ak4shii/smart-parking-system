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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @NotNull
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @ColumnDefault("true")
    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "mqtt_username", unique = true, length = 255)
    private String mqttUsername;

    @Column(name = "mqtt_password_hash", length = 255)
    private String mqttPasswordHash;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
