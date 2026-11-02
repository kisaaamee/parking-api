package com.example.parking.domain.exception;

public final class ParkingFullException extends RuntimeException {

    public ParkingFullException() {
        super("No parking spaces available");
    }
}
