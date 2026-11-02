package com.example.parking.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ParkingBillRequest(@NotBlank(message = "Must not be blank") String vehicleReg) {
}
