package com.example.parking.api;

import com.example.parking.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "parking.capacity=2")
@Import(ParkingHttpIntegrationTest.TestClockConfiguration.class)
class ParkingHttpIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JsonMapper mapper;

    @Test
    @Timeout(30)
    void demonstratesParkingAndBusinessErrorsOverRealHttp() throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            JsonNode initial = exchange(client, "GET", "/parking", null, 200);
            assertEquals(2, initial.get("availableSpaces").intValue());
            assertEquals(0, initial.get("occupiedSpaces").intValue());

            JsonNode entry = exchange(client, "POST", "/parking",
                    "{\"vehicleReg\":\" ab123cd \",\"vehicleType\":2}", 201);
            assertEquals("AB123CD", entry.get("vehicleReg").stringValue());
            assertEquals("2026-11-02T10:00:00Z", entry.get("timeIn").stringValue());
            assertEquals(2, entry.size());

            JsonNode duplicate = exchange(client, "POST", "/parking",
                    "{\"vehicleReg\":\"AB123CD\",\"vehicleType\":1}", 409);
            assertEquals("VEHICLE_ALREADY_PARKED", duplicate.get("code").stringValue());

            exchange(client, "POST", "/parking", "{\"vehicleReg\":\"SECOND\",\"vehicleType\":1}", 201);
            JsonNode full = exchange(client, "POST", "/parking",
                    "{\"vehicleReg\":\"THIRD\",\"vehicleType\":3}", 409);
            assertEquals("PARKING_FULL", full.get("code").stringValue());

            JsonNode invalid = exchange(client, "POST", "/parking",
                    "{\"vehicleReg\":\"INVALID\",\"vehicleType\":4}", 400);
            assertEquals("INVALID_REQUEST", invalid.get("code").stringValue());

            clock.advance(Duration.ofMinutes(7).plusSeconds(20));

            JsonNode bill = exchange(client, "POST", "/parking/bill", "{\"vehicleReg\":\"AB123CD\"}", 200);
            assertTrue(bill.get("vehicleCharge").isNumber());
            assertEquals(0, new BigDecimal("2.60").compareTo(bill.get("vehicleCharge").decimalValue()));
            assertEquals("2026-11-02T10:07:20Z", bill.get("timeOut").stringValue());
            assertEquals(4, bill.size());

            JsonNode afterExit = exchange(client, "GET", "/parking", null, 200);
            assertEquals(1, afterExit.get("availableSpaces").intValue());
            assertEquals(1, afterExit.get("occupiedSpaces").intValue());

            JsonNode repeatedExit = exchange(client, "POST", "/parking/bill",
                    "{\"vehicleReg\":\"AB123CD\"}", 404);
            assertEquals("VEHICLE_NOT_FOUND", repeatedExit.get("code").stringValue());

            exchange(client, "POST", "/parking/bill", "{\"vehicleReg\":\"SECOND\"}", 200);
            JsonNode empty = exchange(client, "GET", "/parking", null, 200);
            assertEquals(2, empty.get("availableSpaces").intValue());
            assertEquals(0, empty.get("occupiedSpaces").intValue());
        }
    }

    private JsonNode exchange(HttpClient client, String method, String path, String body, int expectedStatus)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Accept", "application/json");
        if (body == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        System.out.printf("%nHTTP DEMO: %s %s -> %d%n%s%n", method, path, response.statusCode(), response.body());
        assertEquals(expectedStatus, response.statusCode(), method + " " + path + ": " + response.body());
        return mapper.readTree(response.body());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {

        @Bean
        @Primary
        MutableClock demoClock() {
            return new MutableClock(Instant.parse("2026-11-02T10:00:00Z"));
        }
    }
}
