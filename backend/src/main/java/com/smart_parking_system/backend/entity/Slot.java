package com.smart_parking_system.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "slot")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id", nullable = false)
    private Integer id;

    @Column(name = "name", length = Integer.MAX_VALUE, unique = true)
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ps_id", nullable = false)
    private ParkingSpace ps;

    @ColumnDefault("false")
    @Column(name = "is_occupied")
    private Boolean isOccupied;
}