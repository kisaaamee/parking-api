package com.example.parking.domain;

import java.time.Instant;
import java.util.Objects;

public record ParkingSession(String vehicleReg, VehicleType vehicleType, Instant timeIn) {

    public ParkingSession {
        Objects.requireNonNull(vehicleReg, "vehicleReg must not be null");
        if (vehicleReg.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration must not be blank");
        }
        Objects.requireNonNull(vehicleType, "vehicleType must not be null");
        Objects.requireNonNull(timeIn, "timeIn must not be null");
    }
}
