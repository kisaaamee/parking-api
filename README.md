# Parking API

Java 21 / Spring Boot API with in-memory parking storage.

## Run locally

Install JDK 21 and set `JAVA_HOME`. Maven Wrapper downloads Maven and dependencies
on the first run, which requires internet access.

Linux / macOS:

```sh
chmod +x mvnw
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080/parking`.
Set `PARKING_CAPACITY` to override the default capacity of 100 spaces.

## Tests

- Linux / macOS: `./mvnw clean verify`
- Windows: `.\mvnw.cmd clean verify`
- [Run or view checks in GitHub Actions](https://github.com/kisaaamee/parking-api/actions/workflows/verify.yml)

## Assumptions

- Every vehicle occupies one space; its size affects only the tariff.
- The first available space is the free space with the lowest number.
- Every started minute is billed in full. The GBP 1 surcharge applies to each
  fully completed five-minute interval of actual parking time.
- Exactly zero duration costs zero; there is no minimum parking fee.
- Registrations are trimmed and converted to uppercase for case-insensitive matching.
- Duplicate entries return 409; exiting without an active session returns 404.
- The application runs as one instance. All parking data is lost on restart.
- Timestamps use UTC. The entry response follows the example JSON and omits the
  space number, which is mentioned only in the task's prose.
