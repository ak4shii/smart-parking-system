package com.smart_parking_system.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "rfid")
public class Rfid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rfid_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "rfid_code", nullable = false, length = Integer.MAX_VALUE)
    private String rfidCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ps_id", nullable = false)
    private ParkingSpace ps;

    @ColumnDefault("false")
    @Column(name = "currently_used")
    private Boolean currentlyUsed;
}