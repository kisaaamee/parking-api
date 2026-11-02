package com.example.parking.api.dto;

import com.example.parking.domain.ParkingSession;

import java.time.Instant;

public record ParkVehicleResponse(String vehicleReg, Instant timeIn) {

    public static ParkVehicleResponse from(ParkingSession session) {
        return new ParkVehicleResponse(session.vehicleReg(), session.timeIn());
    }
}
