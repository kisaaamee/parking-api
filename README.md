# Parking API

A Java 21 / Spring Boot project for an in-memory car park management API.

## Current implementation

The backend core is implemented: vehicle types, charge calculation, array-based
parking storage, entry and exit operations, specific business exceptions, and
synchronization for concurrent access. Spring configuration supplies the service,
calculator and UTC clock. HTTP endpoints, React UI, Docker packaging and CI are
planned and are not implemented yet.

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
- Spaces are held in an array inside `ParkingState`. The array is not exposed.
- The first available space means the lowest available space number.
- Capacity is configured with `parking.capacity` (default: 100, must be positive).
- All access to parking state shares one lock within one application instance.
  Each service owns its own state; a mutable state is not shared across services.
- Parking data is lost when the application restarts.
- Registration numbers are stripped of surrounding whitespace and converted to
  uppercase using a locale-independent conversion. Responses use this canonical
  value; matching is case-insensitive. This is an implementation assumption.
- The entry response will follow the example JSON and omit the space number.
  The task's prose mentions this number, but its response example omits it.
- Expected business failures have specific exceptions. The HTTP layer will map
  them to JSON error codes.

## Core behavior and tests

`ParkingService` normalizes registrations, records entry time, calculates an
exit bill and releases the space only after a successful calculation. Time comes
from an injected `Clock`; tests advance a test clock without waiting.

Tests cover vehicle codes and rates, billing boundaries, first-free allocation,
occupancy counts, full parking, duplicate entries, unknown exits, later visits,
and preservation of state after failed operations. Concurrency tests coordinate
two workers to check the last-space, duplicate-entry and duplicate-exit cases.
A Spring context test verifies the configured capacity and service wiring.

The core returns immutable records. The HTTP layer will map these records to
the exact response fields required by the task, and convert integer pence to
GBP using `BigDecimal`.
