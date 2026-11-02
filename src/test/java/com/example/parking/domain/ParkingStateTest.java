package com.example.parking.domain;

import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.domain.exception.VehicleAlreadyParkedException;
import com.example.parking.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingStateTest {

    @Test
    void startsWithAllSpacesAvailable() {
        assertEquals(new ParkingStatus(3, 0), new ParkingState(3).status());
    }

    @Test
    void allocatesLowestAvailableNumberIncludingAFreedSpace() {
        ParkingState state = new ParkingState(3);

        assertEquals(1, state.park(session("FIRST")));
        assertEquals(2, state.park(session("SECOND")));
        state.remove("FIRST");
        assertEquals(1, state.park(session("THIRD")));
        assertEquals(3, state.park(session("FOURTH")));
        assertEquals(new ParkingStatus(0, 3), state.status());
    }

    @Test
    void rejectsEntryWhenFullWithoutReplacingTheExistingSession() {
        ParkingState state = new ParkingState(1);
        ParkingSession existing = session("FIRST");
        state.park(existing);

        assertThrows(ParkingFullException.class, () -> state.park(session("SECOND")));

        assertSame(existing, state.find("FIRST"));
        assertEquals(new ParkingStatus(0, 1), state.status());
    }

    @Test
    void rejectsDuplicateRegistrationWithoutOccupyingAnotherSpace() {
        ParkingState state = new ParkingState(2);
        ParkingSession existing = session("SAME");
        state.park(existing);

        assertThrows(VehicleAlreadyParkedException.class, () -> state.park(session("SAME")));

        assertSame(existing, state.find("SAME"));
        assertEquals(new ParkingStatus(1, 1), state.status());
    }

    @Test
    void missingVehicleDoesNotChangeOtherParkingSessions() {
        ParkingState state = new ParkingState(2);
        ParkingSession existing = session("FIRST");
        state.park(existing);

        assertThrows(VehicleNotFoundException.class, () -> state.find("UNKNOWN"));
        assertThrows(VehicleNotFoundException.class, () -> state.remove("UNKNOWN"));

        assertSame(existing, state.find("FIRST"));
        assertEquals(new ParkingStatus(1, 1), state.status());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThrows(IllegalArgumentException.class, () -> new ParkingState(capacity));
    }

    private static ParkingSession session(String registration) {
        return new ParkingSession(registration, VehicleType.SMALL, Instant.parse("2026-11-02T10:00:00Z"));
    }
}
