package com.example.parking.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ApiError(String code, String message,
                       @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldValidationError> errors) {

    public ApiError {
        errors = List.copyOf(errors);
    }

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }
}
