package com.example.parking.api.error;

import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.domain.exception.VehicleAlreadyParkedException;
import com.example.parking.domain.exception.VehicleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ParkingFullException.class)
    ResponseEntity<ApiError> parkingFull(ParkingFullException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("PARKING_FULL", exception.getMessage()));
    }

    @ExceptionHandler(VehicleAlreadyParkedException.class)
    ResponseEntity<ApiError> vehicleAlreadyParked(VehicleAlreadyParkedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("VEHICLE_ALREADY_PARKED", exception.getMessage()));
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    ResponseEntity<ApiError> vehicleNotFound(VehicleNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("VEHICLE_NOT_FOUND", exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field).thenComparing(FieldValidationError::message))
                .toList();
        return handleExceptionInternal(exception,
                new ApiError("INVALID_REQUEST", "Request validation failed", errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return handleExceptionInternal(exception,
                new ApiError("INVALID_REQUEST", "Request body must be valid JSON with the required field types"),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
                                                             HttpHeaders headers, HttpStatusCode status,
                                                             WebRequest request) {
        if (!(body instanceof ApiError)) {
            String code = switch (status.value()) {
                case 400 -> "INVALID_REQUEST";
                case 404 -> "NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 406 -> "NOT_ACCEPTABLE";
                case 415 -> "UNSUPPORTED_MEDIA_TYPE";
                default -> "REQUEST_FAILED";
            };
            HttpStatus httpStatus = HttpStatus.resolve(status.value());
            String message = httpStatus == null ? "Request failed" : httpStatus.getReasonPhrase();
            body = new ApiError(code, message);
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedFailure(Exception exception) {
        log.error("Unexpected failure while processing parking request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
