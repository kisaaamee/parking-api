package com.example.parking.domain.exception;

public final class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String vehicleReg) {
        super("Vehicle is not parked: " + vehicleReg);
    }
}
