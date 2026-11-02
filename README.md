# Parking API

A Java 21 / Spring Boot project for an in-memory car park management API.

## Current implementation

This is the first implementation slice: the Spring Boot project setup, Maven
Wrapper, vehicle types and parking charge calculator with unit tests.
Parking storage, HTTP endpoints, React UI, Docker packaging and CI are planned
and are not implemented yet.

## Run the checks

Install JDK 21 and set `JAVA_HOME` to its installation directory. Maven does not
need to be installed separately. The first run requires internet access to
download Maven and project dependencies.

Linux / macOS:

```sh
chmod +x mvnw
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

## Billing rules

| Vehicle type | API code | Rate per started minute |
| --- | --- | --- |
| Small | 1 | GBP 0.10 |
| Medium | 2 | GBP 0.20 |
| Large | 3 | GBP 0.40 |

- Each started minute is billed in full, including fractions of a second.
- GBP 1.00 is added for every fully completed five-minute interval of actual
  parking time. The surcharge is independent of rounding for minute billing.
- Exactly zero duration costs zero. No minimum stay or minimum fee is specified
  by the task; this is an implementation assumption.
- Negative durations are rejected.
- Monetary calculations use integer pence.

For example, a medium car parked for 7 minutes 20 seconds costs
`8 * 20 + 1 * 100 = 260` pence, or GBP 2.60.

## Design decisions agreed so far

- One vehicle occupies one space; vehicle size only affects the tariff.
- Spaces will be held in an array inside `ParkingState`.
- The first available space means the lowest available space number.
- Capacity will be configurable, with a proposed default of 100 spaces.
- Parking operations will share a lock within one application instance.
- Parking data will be lost when the application restarts.
- The entry response will follow the example JSON and omit the space number.
  The task's prose mentions this number, but its response example omits it.
- Expected business failures will have specific exceptions and JSON error codes.

The tests currently cover vehicle type codes, all three rates, minute and
five-minute boundaries, sub-second durations and invalid negative durations.
