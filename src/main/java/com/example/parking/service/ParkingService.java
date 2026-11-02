package com.example.parking.service;

import com.example.parking.domain.ParkingBill;
import com.example.parking.domain.ParkingSession;
import com.example.parking.domain.ParkingState;
import com.example.parking.domain.ParkingStatus;
import com.example.parking.domain.VehicleType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class ParkingService {

    private final Object lock = new Object();
    private final ParkingState state;
    private final Clock clock;
    private final ParkingChargeCalculator chargeCalculator;

    public ParkingService(int capacity, Clock clock, ParkingChargeCalculator chargeCalculator) {
        this.state = new ParkingState(capacity);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.chargeCalculator = Objects.requireNonNull(chargeCalculator, "chargeCalculator must not be null");
    }

    public ParkingStatus status() {
        synchronized (lock) {
            return state.status();
        }
    }

    public ParkingSession park(String vehicleReg, VehicleType vehicleType) {
        String registration = normalizeRegistration(vehicleReg);
        Objects.requireNonNull(vehicleType, "vehicleType must not be null");
        synchronized (lock) {
            ParkingSession session = new ParkingSession(registration, vehicleType, clock.instant());
            state.park(session);
            return session;
        }
    }

    public ParkingBill bill(String vehicleReg) {
        String registration = normalizeRegistration(vehicleReg);
        synchronized (lock) {
            ParkingSession session = state.find(registration);
            Instant timeOut = clock.instant();
            Duration duration = Duration.between(session.timeIn(), timeOut);
            long chargePence = chargeCalculator.calculatePence(session.vehicleType(), duration);
            ParkingBill bill = new ParkingBill(registration, chargePence, session.timeIn(), timeOut);
            state.remove(registration);
            return bill;
        }
    }

    private static String normalizeRegistration(String vehicleReg) {
        Objects.requireNonNull(vehicleReg, "vehicleReg must not be null");
        if (vehicleReg.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration must not be blank");
        }
        return vehicleReg.strip().toUpperCase(Locale.ROOT);
    }
}
