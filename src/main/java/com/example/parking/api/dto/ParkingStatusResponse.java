package com.example.parking.api.dto;

import com.example.parking.domain.ParkingStatus;

public record ParkingStatusResponse(int availableSpaces, int occupiedSpaces) {

    public static ParkingStatusResponse from(ParkingStatus status) {
        return new ParkingStatusResponse(status.availableSpaces(), status.occupiedSpaces());
    }
}
