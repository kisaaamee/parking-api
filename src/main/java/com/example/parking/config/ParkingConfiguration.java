package com.example.parking.config;

import com.example.parking.service.ParkingChargeCalculator;
import com.example.parking.service.ParkingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ParkingConfiguration {

    @Bean
    Clock parkingClock() {
        return Clock.systemUTC();
    }

    @Bean
    ParkingChargeCalculator parkingChargeCalculator() {
        return new ParkingChargeCalculator();
    }

    @Bean
    ParkingService parkingService(@Value("${parking.capacity:100}") int capacity,
                                  Clock clock,
                                  ParkingChargeCalculator chargeCalculator) {
        return new ParkingService(capacity, clock, chargeCalculator);
    }
}
