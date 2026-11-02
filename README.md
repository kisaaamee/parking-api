# Parking API

A Java 21 / Spring Boot project for an in-memory car park management API.

## Current implementation

The backend core is implemented: vehicle types, charge calculation, array-based
parking storage, entry and exit operations, specific business exceptions, and
synchronization for concurrent access. The three HTTP endpoints, request
validation and centralized JSON errors are implemented and tested. Spring
configuration supplies the service, calculator and UTC clock. GitHub Actions is
configured to build and test the application on Java 21, including an HTTP
demonstration using a real server. Test reports and the application JAR are saved
as workflow artifacts.

## Run from GitHub

[Open Parking API checks on GitHub](https://github.com/kisaaamee/parking-api/actions/workflows/verify.yml).

The workflow runs automatically on pushes and pull requests. To start it manually:

1. Push the project, including `.github/workflows/verify.yml`, to the repository's
   default branch.
2. Open **Actions** and select **Parking API checks**.
3. Choose **Run workflow** and start the run.
4. Open **Build and test on Java 21** to inspect the build and test output.
5. Find `HTTP DEMO` in the build step log to see actual HTTP requests and JSON
   responses. Download `test-reports` for JUnit reports, or `parking-api` for the JAR.

Manual runs require write access to the repository. Reviewers with read access
can inspect the results of completed runs. No local installation is needed to
view the workflow results. The workflow starts a temporary server for its tests;
it does not publish a permanently running API.

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

The suite currently contains 74 tests. The full build, tests and live HTTP
demonstration have been verified on Temurin JDK 21.0.13.

## Run the API

Linux / macOS:

```sh
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The server listens on `http://localhost:8080`. Set `PARKING_CAPACITY` or the
`parking.capacity` Spring property to override the default capacity of 100.
There are no external runtime services to start.

### API examples (PowerShell)

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/parking'

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/parking' `
  -ContentType 'application/json' `
  -Body '{"vehicleReg":"AB123CD","vehicleType":2}'

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/parking/bill' `
  -ContentType 'application/json' `
  -Body '{"vehicleReg":"AB123CD"}'
```

| Endpoint | Success status | Response fields |
| --- | --- | --- |
| `GET /parking` | 200 | `availableSpaces`, `occupiedSpaces` |
| `POST /parking` | 201 | `vehicleReg`, `timeIn` |
| `POST /parking/bill` | 200 | `vehicleReg`, `vehicleCharge`, `timeIn`, `timeOut` |

Timestamps are ISO-8601 UTC strings. `vehicleCharge` is a JSON number in GBP.
Billing also closes the session; repeating a successful exit returns 404.

### Validation and errors

`vehicleReg` must be a nonblank JSON string. `vehicleType` must be a JSON integer
equal to 1, 2 or 3; strings such as `"1"` and fractional numbers are rejected.

| HTTP status | Error code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Invalid fields, missing body or unreadable JSON |
| 404 | `VEHICLE_NOT_FOUND` | No active session for the vehicle |
| 409 | `VEHICLE_ALREADY_PARKED` | Registration already has an active session |
| 409 | `PARKING_FULL` | No available spaces |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | POST body is not sent as JSON |
| 500 | `INTERNAL_ERROR` | Unexpected failure; details are logged on the server |

Errors contain a stable `code` and a human-readable `message`. Field validation
errors additionally contain an `errors` array with `field` and `message` entries.

```json
{
  "code": "PARKING_FULL",
  "message": "No parking spaces available"
}
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

## Architecture

Requests pass through `ParkingController` and validated request DTOs to
`ParkingService`. The service owns `ParkingState`, whose private array stores
active parking sessions. `ParkingChargeCalculator` calculates charges without
changing parking state. Response DTOs define the public JSON contract, and
`GlobalExceptionHandler` translates failures into HTTP errors.

One shared service lock protects each complete entry, exit or status operation.
The service returns immutable records and never exposes its mutable array.
Allocation, registration lookup and occupancy counting take O(capacity) time.
This keeps one source of truth for a small, fixed-capacity car park. A larger
system could introduce indexes after measuring its workload.

## Assumptions and design decisions

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
- The entry response follows the example JSON and omits the space number.
  The task's prose mentions this number, but its response example omits it.
- Expected business failures have specific exceptions. The HTTP layer maps
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

The core returns immutable records. The HTTP layer maps these records to
the exact response fields required by the task, and converts integer pence to
GBP using `BigDecimal`.

`ParkingApiTest` uses Spring's real application context and MockMvc. Requests
pass through routing, JSON conversion, validation, controllers, the real parking
service and error handlers without opening a network port. Each test receives
a fresh application context and a controllable clock, so no parking data or
test time leaks between cases. Tests use real collaborators rather than Mockito.

`ParkingHttpIntegrationTest` starts the embedded server on a random port and uses
Java's HTTP client to demonstrate availability, entry, billing and business
errors. Its test clock advances instantly by 7 minutes 20 seconds, producing a
repeatable GBP 2.60 bill for a medium car. Requests and responses are printed in
the test output, making the scenario visible in GitHub Actions.
