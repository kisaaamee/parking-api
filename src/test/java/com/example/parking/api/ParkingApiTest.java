package com.example.parking.api;

import com.example.parking.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "parking.capacity=2")
@Import(ParkingApiTest.TestClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ParkingApiTest {

    private static final String TIME_IN = "2026-11-02T10:00:00Z";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MutableClock clock;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void entryAndExitFollowTheExactResponseContractAndUpdateOccupancy() throws Exception {
        expectEmptyParking();

        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\" ab123cd \",\"vehicleType\":2}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.vehicleReg").value("AB123CD"))
                .andExpect(jsonPath("$.timeIn").value(TIME_IN));

        mvc.perform(get("/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSpaces").value(1))
                .andExpect(jsonPath("$.occupiedSpaces").value(1));

        clock.advance(Duration.ofMinutes(7).plusSeconds(20));

        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"ab123cd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.vehicleReg").value("AB123CD"))
                .andExpect(jsonPath("$.vehicleCharge").isNumber())
                .andExpect(jsonPath("$.vehicleCharge").value(2.60))
                .andExpect(jsonPath("$.timeIn").value(TIME_IN))
                .andExpect(jsonPath("$.timeOut").value("2026-11-02T10:07:20Z"));

        expectEmptyParking();
    }

    @Test
    void duplicateEntryReturnsItsSpecificErrorAndKeepsTheOriginalSession() throws Exception {
        park("AB123CD", 1);

        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"ab123cd\",\"vehicleType\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ALREADY_PARKED"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        clock.advance(Duration.ofMinutes(5));

        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"AB123CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCharge").value(1.50))
                .andExpect(jsonPath("$.timeIn").value(TIME_IN));
    }

    @Test
    void fullParkingReturnsItsSpecificErrorAndFreedSpaceCanBeUsedAgain() throws Exception {
        park("FIRST", 1);
        park("SECOND", 2);

        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"THIRD\",\"vehicleType\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARKING_FULL"));

        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"FIRST\"}"))
                .andExpect(status().isOk());
        park("THIRD", 3);

        mvc.perform(get("/parking"))
                .andExpect(jsonPath("$.availableSpaces").value(0))
                .andExpect(jsonPath("$.occupiedSpaces").value(2));
    }

    @Test
    void repeatedExitReturnsVehicleNotFound() throws Exception {
        park("AB123CD", 1);
        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"AB123CD\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"AB123CD\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        expectEmptyParking();
    }

    @Test
    void validationErrorsIdentifyTheInvalidFields() throws Exception {
        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\" \",\"vehicleType\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("vehicleReg", "vehicleType")));
        expectEmptyParking();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"vehicleReg\":null,\"vehicleType\":1}",
            "{\"vehicleReg\":\"A\"}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":null}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":4}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":-1}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":1.5}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":\"1\"}",
            "{\"vehicleReg\":\"A\",\"vehicleType\":true}",
            "{\"vehicleReg\":123,\"vehicleType\":1}",
            "{\"vehicleReg\":true,\"vehicleType\":1}",
            "{\"vehicleReg\":[],\"vehicleType\":1}",
            "null",
            "{broken",
            ""
    })
    void invalidEntryRequestsReturn400WithoutChangingState(String body) throws Exception {
        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        expectEmptyParking();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"vehicleReg\":\" \"}", "{\"vehicleReg\":123}", "null", "{broken"})
    void invalidExitRequestsReturn400WithoutClosingAnySession(String body) throws Exception {
        park("FIRST", 1);
        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/parking"))
                .andExpect(jsonPath("$.occupiedSpaces").value(1));
    }

    @Test
    void unsupportedContentTypeAndMethodUseTheSameErrorFormat() throws Exception {
        mvc.perform(post("/parking").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(delete("/parking"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        expectEmptyParking();
    }

    @Test
    void unknownRouteUsesTheSameErrorFormat() throws Exception {
        mvc.perform(get("/parking/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unexpectedCalculationFailureReturns500AndPreservesTheSession() throws Exception {
        park("AB123CD", 1);
        clock.advance(Duration.ofSeconds(-1));

        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"AB123CD\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        mvc.perform(get("/parking"))
                .andExpect(jsonPath("$.occupiedSpaces").value(1));
        clock.advance(Duration.ofSeconds(2));
        mvc.perform(post("/parking/bill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"AB123CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCharge").value(0.10));
    }

    private void park(String registration, int type) throws Exception {
        mvc.perform(post("/parking").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleReg\":\"" + registration + "\",\"vehicleType\":" + type + "}"))
                .andExpect(status().isCreated());
    }

    private void expectEmptyParking() throws Exception {
        mvc.perform(get("/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.availableSpaces").value(2))
                .andExpect(jsonPath("$.occupiedSpaces").value(0));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse(TIME_IN));
        }
    }
}
