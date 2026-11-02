package com.example.parking.api.dto;

import com.example.parking.domain.ParkingBill;

import java.math.BigDecimal;
import java.time.Instant;

public record ParkingBillResponse(String vehicleReg, BigDecimal vehicleCharge, Instant timeIn, Instant timeOut) {

    public static ParkingBillResponse from(ParkingBill bill) {
        return new ParkingBillResponse(bill.vehicleReg(), BigDecimal.valueOf(bill.chargePence(), 2),
                bill.timeIn(), bill.timeOut());
    }
}
