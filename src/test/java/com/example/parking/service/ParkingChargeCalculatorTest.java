package com.example.parking.service;

import com.example.parking.domain.VehicleType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingChargeCalculatorTest {

    private final ParkingChargeCalculator calculator = new ParkingChargeCalculator();

    @ParameterizedTest(name = "small car: {0} costs {1} pence")
    @CsvSource({
            "PT0S, 0",
            "PT0.000000001S, 10",
            "PT1S, 10",
            "PT59.999999999S, 10",
            "PT1M, 10",
            "PT1M0.000000001S, 20",
            "PT4M59.999999999S, 50",
            "PT5M, 150",
            "PT5M0.000000001S, 160",
            "PT9M59.999999999S, 200",
            "PT10M, 300",
            "PT10M0.000000001S, 310"
    })
    void billsStartedMinutesAndCompletedFiveMinuteIntervals(String duration, long expectedPence) {
        long charge = calculator.calculatePence(VehicleType.SMALL, Duration.parse(duration));

        assertEquals(expectedPence, charge);
    }

    @ParameterizedTest
    @CsvSource({"SMALL, 180", "MEDIUM, 260", "LARGE, 420"})
    void appliesTheVehicleRateAndSurcharge(VehicleType type, long expectedPence) {
        long charge = calculator.calculatePence(type, Duration.ofMinutes(7).plusSeconds(20));

        assertEquals(expectedPence, charge);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-0.000000001S", "PT-1S", "PT-1M"})
    void rejectsNegativeParkingDuration(String duration) {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculatePence(VehicleType.SMALL, Duration.parse(duration)));
    }
}
