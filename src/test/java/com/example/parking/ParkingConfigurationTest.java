package com.example.parking;

import com.example.parking.domain.ParkingStatus;
import com.example.parking.domain.VehicleType;
import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.service.ParkingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "parking.capacity=2")
class ParkingConfigurationTest {

    @Autowired
    private ParkingService service;

    @Test
    void wiresTheServiceWithTheConfiguredCapacity() {
        assertEquals(new ParkingStatus(2, 0), service.status());

        service.park("FIRST", VehicleType.SMALL);
        service.park("SECOND", VehicleType.LARGE);

        assertThrows(ParkingFullException.class, () -> service.park("THIRD", VehicleType.MEDIUM));
        assertEquals(new ParkingStatus(0, 2), service.status());
    }
}
