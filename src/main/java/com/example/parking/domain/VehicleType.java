package com.example.parking.domain;

public enum VehicleType {
    SMALL(1, 10),
    MEDIUM(2, 20),
    LARGE(3, 40);

    private final int code;
    private final long ratePencePerMinute;

    VehicleType(int code, long ratePencePerMinute) {
        this.code = code;
        this.ratePencePerMinute = ratePencePerMinute;
    }

    public int code() {
        return code;
    }

    public long ratePencePerMinute() {
        return ratePencePerMinute;
    }

    public static VehicleType fromCode(int code) {
        return switch (code) {
            case 1 -> SMALL;
            case 2 -> MEDIUM;
            case 3 -> LARGE;
            default -> throw new IllegalArgumentException("Vehicle type must be 1, 2 or 3");
        };
    }
}
