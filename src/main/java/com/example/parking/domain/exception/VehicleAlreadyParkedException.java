package com.example.parking.domain.exception;

public final class VehicleAlreadyParkedException extends RuntimeException {

    public VehicleAlreadyParkedException(String vehicleReg) {
        super("Vehicle is already parked: " + vehicleReg);
    }
}
