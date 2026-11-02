package com.example.parking.service;

import com.example.parking.domain.ParkingBill;
import com.example.parking.domain.ParkingSession;
import com.example.parking.domain.ParkingStatus;
import com.example.parking.domain.VehicleType;
import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.domain.exception.VehicleAlreadyParkedException;
import com.example.parking.domain.exception.VehicleNotFoundException;
import com.example.parking.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingServiceTest {

    private static final Instant TIME_IN = Instant.parse("2026-11-02T10:00:00Z");
    private final MutableClock clock = new MutableClock(TIME_IN);
    private final ParkingService service = new ParkingService(2, clock, new ParkingChargeCalculator());

    @Test
    void entryRecordsTimeAndUpdatesOccupancy() {
        ParkingSession session = service.park("AB123CD", VehicleType.MEDIUM);

        assertEquals(new ParkingSession("AB123CD", VehicleType.MEDIUM, TIME_IN), session);
        assertEquals(new ParkingStatus(1, 1), service.status());
    }

    @Test
    void exitUsesRecordedEntryTimeAndRateAndFreesTheSpace() {
        service.park("AB123CD", VehicleType.MEDIUM);
        clock.advance(Duration.ofMinutes(7).plusSeconds(20));

        ParkingBill bill = service.bill("AB123CD");

        assertEquals(new ParkingBill("AB123CD", 260, TIME_IN, TIME_IN.plusSeconds(440)), bill);
        assertEquals(new ParkingStatus(2, 0), service.status());
        assertThrows(VehicleNotFoundException.class, () -> service.bill("AB123CD"));
    }

    @Test
    void aLaterVisitStartsANewSessionWithItsOwnRate() {
        service.park("AB123CD", VehicleType.SMALL);
        clock.advance(Duration.ofMinutes(5));
        assertEquals(150, service.bill("AB123CD").chargePence());

        clock.advance(Duration.ofHours(1));
        Instant secondEntry = clock.instant();
        ParkingSession secondSession = service.park("AB123CD", VehicleType.LARGE);
        clock.advance(Duration.ofSeconds(1));

        assertEquals(secondEntry, secondSession.timeIn());
        assertEquals(new ParkingBill("AB123CD", 40, secondEntry, secondEntry.plusSeconds(1)),
                service.bill("AB123CD"));
    }

    @Test
    void registrationMatchingIgnoresCaseAndSurroundingWhitespace() {
        ParkingSession session = service.park("  ab123cd  ", VehicleType.SMALL);

        assertEquals("AB123CD", session.vehicleReg());
        assertThrows(VehicleAlreadyParkedException.class,
                () -> service.park("Ab123Cd", VehicleType.LARGE));
        assertEquals("AB123CD", service.bill(" ab123cd ").vehicleReg());
    }

    @Test
    void failedEntryAndUnknownExitLeaveStateUnchanged() {
        service.park("FIRST", VehicleType.SMALL);
        service.park("SECOND", VehicleType.LARGE);

        assertThrows(ParkingFullException.class, () -> service.park("THIRD", VehicleType.MEDIUM));
        assertThrows(VehicleNotFoundException.class, () -> service.bill("UNKNOWN"));

        assertEquals(new ParkingStatus(0, 2), service.status());
        assertEquals("FIRST", service.bill("FIRST").vehicleReg());
        assertEquals("SECOND", service.bill("SECOND").vehicleReg());
    }

    @Test
    void calculationFailureDoesNotRemoveTheParkedVehicle() {
        service.park("AB123CD", VehicleType.SMALL);
        clock.advance(Duration.ofSeconds(-1));

        assertThrows(IllegalArgumentException.class, () -> service.bill("AB123CD"));
        assertEquals(new ParkingStatus(1, 1), service.status());

        clock.advance(Duration.ofSeconds(2));
        assertEquals(10, service.bill("AB123CD").chargePence());
        assertEquals(new ParkingStatus(2, 0), service.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void blankRegistrationsAreRejectedWithoutChangingState(String registration) {
        assertThrows(IllegalArgumentException.class, () -> service.park(registration, VehicleType.SMALL));
        assertThrows(IllegalArgumentException.class, () -> service.bill(registration));
        assertEquals(new ParkingStatus(2, 0), service.status());
    }
}
