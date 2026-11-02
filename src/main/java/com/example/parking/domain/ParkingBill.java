package com.example.parking.domain;

import java.time.Instant;

public record ParkingBill(String vehicleReg, long chargePence, Instant timeIn, Instant timeOut) {
}
