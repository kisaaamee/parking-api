package com.example.parking.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleTypeTest {

    @ParameterizedTest
    @CsvSource({"1, SMALL, 10", "2, MEDIUM, 20", "3, LARGE, 40"})
    void mapsApiCodeToVehicleTypeAndRate(int code, VehicleType expectedType, long expectedRate) {
        VehicleType type = VehicleType.fromCode(code);

        assertEquals(expectedType, type);
        assertEquals(code, type.code());
        assertEquals(expectedRate, type.ratePencePerMinute());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 4, 100})
    void rejectsUnsupportedVehicleType(int code) {
        assertThrows(IllegalArgumentException.class, () -> VehicleType.fromCode(code));
    }
}
