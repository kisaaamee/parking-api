package com.example.parking.service;

import com.example.parking.domain.ParkingStatus;
import com.example.parking.domain.VehicleType;
import com.example.parking.domain.exception.ParkingFullException;
import com.example.parking.domain.exception.VehicleAlreadyParkedException;
import com.example.parking.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(15)
class ParkingServiceConcurrencyTest {

    @Test
    void onlyOneVehicleCanTakeTheLastSpace() throws Exception {
        ParkingService service = service(1);

        var outcomes = runConcurrently(
                () -> service.park("FIRST", VehicleType.SMALL),
                () -> service.park("SECOND", VehicleType.LARGE));

        assertOneSuccessAndOneFailure(outcomes, ParkingFullException.class);
        assertEquals(new ParkingStatus(0, 1), service.status());
    }

    @Test
    void theSameVehicleCannotOccupyTwoSpaces() throws Exception {
        ParkingService service = service(2);

        var outcomes = runConcurrently(
                () -> service.park("SAME", VehicleType.SMALL),
                () -> service.park("SAME", VehicleType.SMALL));

        assertOneSuccessAndOneFailure(outcomes, VehicleAlreadyParkedException.class);
        assertEquals(new ParkingStatus(1, 1), service.status());
    }

    @Test
    void theSameParkingSessionCanOnlyBeClosedOnce() throws Exception {
        ParkingService service = service(1);
        service.park("SAME", VehicleType.SMALL);

        var outcomes = runConcurrently(() -> service.bill("SAME"), () -> service.bill("SAME"));

        assertOneSuccessAndOneFailure(outcomes, VehicleNotFoundException.class);
        assertEquals(new ParkingStatus(1, 0), service.status());
    }

    private static ParkingService service(int capacity) {
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
        return new ParkingService(capacity, clock, new ParkingChargeCalculator());
    }

    private static <T> List<Outcome<T>> runConcurrently(Supplier<T> first, Supplier<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Outcome<T>> firstResult = executor.submit(attempt(first, ready, start));
            Future<Outcome<T>> secondResult = executor.submit(attempt(second, ready, start));
            try {
                assertTrue(ready.await(5, SECONDS), "Both workers must be ready");
                start.countDown();
                return List.of(firstResult.get(5, SECONDS), secondResult.get(5, SECONDS));
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }
    }

    private static <T> Callable<Outcome<T>> attempt(Supplier<T> operation,
                                                   CountDownLatch ready,
                                                   CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, SECONDS)) {
                throw new TimeoutException("Workers were not released");
            }
            try {
                return new Outcome<>(operation.get(), null);
            } catch (RuntimeException exception) {
                return new Outcome<>(null, exception);
            }
        };
    }

    private static void assertOneSuccessAndOneFailure(List<? extends Outcome<?>> outcomes,
                                                      Class<? extends RuntimeException> failureType) {
        assertEquals(1, outcomes.stream().filter(outcome -> outcome.failure() == null).count());
        assertEquals(1, outcomes.stream().filter(outcome -> failureType.isInstance(outcome.failure())).count());
    }

    private record Outcome<T>(T result, RuntimeException failure) {
    }
}
