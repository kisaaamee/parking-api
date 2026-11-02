package com.example.parking.api;

import com.example.parking.api.dto.ParkVehicleRequest;
import com.example.parking.api.dto.ParkVehicleResponse;
import com.example.parking.api.dto.ParkingBillRequest;
import com.example.parking.api.dto.ParkingBillResponse;
import com.example.parking.api.dto.ParkingStatusResponse;
import com.example.parking.domain.VehicleType;
import com.example.parking.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/parking", produces = MediaType.APPLICATION_JSON_VALUE)
public class ParkingController {

    private final ParkingService service;

    public ParkingController(ParkingService service) {
        this.service = service;
    }

    @GetMapping
    public ParkingStatusResponse status() {
        return ParkingStatusResponse.from(service.status());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ParkVehicleResponse park(@Valid @RequestBody ParkVehicleRequest request) {
        return ParkVehicleResponse.from(service.park(request.vehicleReg(), VehicleType.fromCode(request.vehicleType())));
    }

    @PostMapping(value = "/bill", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ParkingBillResponse bill(@Valid @RequestBody ParkingBillRequest request) {
        return ParkingBillResponse.from(service.bill(request.vehicleReg()));
    }
}
