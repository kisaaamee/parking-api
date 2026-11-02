package com.example.parking.domain;

import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.domain.exception.VehicleAlreadyParkedException;
import com.example.parking.domain.exception.VehicleNotFoundException;

import java.util.Objects;

/** Mutable storage owned by one service, which synchronizes every access. */
public final class ParkingState {

    private final ParkingSession[] spaces;

    public ParkingState(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Parking capacity must be positive");
        }
        spaces = new ParkingSession[capacity];
    }

    public ParkingStatus status() {
        int occupiedSpaces = 0;
        for (ParkingSession session : spaces) {
            if (session != null) {
                occupiedSpaces++;
            }
        }
        return new ParkingStatus(spaces.length - occupiedSpaces, occupiedSpaces);
    }

    public int park(ParkingSession session) {
        Objects.requireNonNull(session, "session must not be null");
        if (indexOf(session.vehicleReg()) >= 0) {
            throw new VehicleAlreadyParkedException(session.vehicleReg());
        }
        for (int index = 0; index < spaces.length; index++) {
            if (spaces[index] == null) {
                spaces[index] = session;
                return index + 1;
            }
        }
        throw new ParkingFullException();
    }

    public ParkingSession find(String vehicleReg) {
        return spaces[requireIndexOf(vehicleReg)];
    }

    public void remove(String vehicleReg) {
        spaces[requireIndexOf(vehicleReg)] = null;
    }

    private int requireIndexOf(String vehicleReg) {
        int index = indexOf(vehicleReg);
        if (index < 0) {
            throw new VehicleNotFoundException(vehicleReg);
        }
        return index;
    }

    private int indexOf(String vehicleReg) {
        for (int index = 0; index < spaces.length; index++) {
            ParkingSession session = spaces[index];
            if (session != null && session.vehicleReg().equals(vehicleReg)) {
                return index;
            }
        }
        return -1;
    }
}
