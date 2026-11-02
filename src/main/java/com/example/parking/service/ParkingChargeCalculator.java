package com.example.parking.service;

import com.example.parking.domain.VehicleType;

import java.time.Duration;
import java.util.Objects;

public final class ParkingChargeCalculator {

    private static final long SURCHARGE_INTERVAL_MINUTES = 5;
    private static final long SURCHARGE_PENCE = 100;

    public long calculatePence(VehicleType vehicleType, Duration duration) {
        Objects.requireNonNull(vehicleType, "vehicleType must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Parking duration must not be negative");
        }

        long completedMinutes = duration.toMinutes();
        boolean hasPartialMinute = !duration.minusMinutes(completedMinutes).isZero();
        long billableMinutes = completedMinutes + (hasPartialMinute ? 1 : 0);
        long surchargeIntervals = completedMinutes / SURCHARGE_INTERVAL_MINUTES;

        long baseCharge = Math.multiplyExact(billableMinutes, vehicleType.ratePencePerMinute());
        long surcharge = Math.multiplyExact(surchargeIntervals, SURCHARGE_PENCE);
        return Math.addExact(baseCharge, surcharge);
    }
}
