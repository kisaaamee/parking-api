package com.example.parking.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParkVehicleRequest(
        @NotBlank(message = "Must not be blank") String vehicleReg,
        @NotNull(message = "Must be provided")
        @Min(value = 1, message = "Must be one of: 1, 2, 3")
        @Max(value = 3, message = "Must be one of: 1, 2, 3") Integer vehicleType) {
}
