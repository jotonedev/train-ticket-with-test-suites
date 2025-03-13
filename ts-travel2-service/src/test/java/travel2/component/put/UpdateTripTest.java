package travel2.component.put;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import travel2.entity.*;
import travel2.repository.TripRepository;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateTripTest {
    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MockMvc mockMvc;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
    }

    @BeforeEach
    void beforeEach() {
        tripRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a Trip that does exist in the database
     * <li><b>Parameters:</b></li> A Trip with an ID that matches the ID of a Trip in the database
     * <li><b>Expected result:</b></li> status 1, msg "Update Trip", the updated Trip. The database contains the updated Trip.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateValidObject() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TravelInfo updateTravelInfo = configureTravelInfo();
        updateTravelInfo.setTripId(storedTrip.getTripId().toString());

        // Act
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update trip info:" + updateTravelInfo.getTripId())))
                .andExpect(jsonPath("$.data.tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data.tripId.type", is(storedTrip.getTripId().getType().toString())))

                // updateTravelInfo has new values that should be returned on update
                .andExpect(jsonPath("$.data.routeId", is(updateTravelInfo.getRouteId())))
                .andExpect(jsonPath("$.data.trainTypeId", is(updateTravelInfo.getTrainTypeId())))
                .andExpect(jsonPath("$.data.startingStationId", is(updateTravelInfo.getStartingStationId())))
                .andExpect(jsonPath("$.data.stationsId", is(updateTravelInfo.getStationsId())))
                .andExpect(jsonPath("$.data.terminalStationId", is(updateTravelInfo.getTerminalStationId())));

        // Make sure the trip was updated in the database
        assertEquals(1L, tripRepository.count());
        TripId tripId = new TripId(updateTravelInfo.getTripId());
        Optional<Trip> OptionalTrip = tripRepository.findById(tripId);
        assertTrue(OptionalTrip.isPresent());
        Trip trip = OptionalTrip.get();
        assertEquals(updateTravelInfo.getTripId(), trip.getTripId().toString());
        assertEquals(updateTravelInfo.getTrainTypeId(), trip.getTrainTypeId());
        assertEquals(updateTravelInfo.getStartingStationId(), trip.getStartingStationId());
        assertEquals(updateTravelInfo.getStationsId(), trip.getStationsId());
        assertEquals(updateTravelInfo.getTerminalStationId(), trip.getTerminalStationId());
        assertEquals(updateTravelInfo.getRouteId(), trip.getRouteId());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TravelInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateMultipleObjects() throws Exception {
        // Arrange
        TravelInfo[] info = {configureTravelInfo(), configureTravelInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a Trip that does not already exists in the database
     * <li><b>Parameters:</b></li> a Trip with a random id
     * <li><b>Expected result:</b></li> status 0, msg "Trip doesn't exist", no data
     * <li><b>Related Issue:</b></li> <b>F1a:</b> The logic of the implementation is correct, but the wrong status code
     * is returned anyway.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testUpdateMissingObject() throws Exception {
        // Arrange
        TravelInfo travelInfo = configureTravelInfo();

        // Act
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Trip" + travelInfo.getTripId() + "doesn 't exists")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"some\":wrong, \"request\":json, \"string\":content}";

        // Act
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    void testUpdateEmptyBody() throws Exception {
        // Act
        String requestJson = "";
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private Trip populateDatabase() {
        Trip trip = new Trip();
        TripId tripId = new TripId();
        tripId.setType(Type.Z);
        tripId.setNumber(UUID.randomUUID().toString());
        trip.setTripId(tripId);
        trip.setRouteId(UUID.randomUUID().toString());
        trip.setStartingTime(new Date());
        return tripRepository.save(trip);
    }

    private TravelInfo configureTravelInfo() {
        TravelInfo travelInfo = new TravelInfo();
        travelInfo.setTripId(UUID.randomUUID().toString());
        travelInfo.setTrainTypeId(UUID.randomUUID().toString());
        travelInfo.setStartingStationId(UUID.randomUUID().toString());
        travelInfo.setStationsId(UUID.randomUUID().toString());
        travelInfo.setTerminalStationId(UUID.randomUUID().toString());
        travelInfo.setRouteId(UUID.randomUUID().toString());

        TripId tripId = new TripId();
        tripId.setType(Type.Z);
        tripId.setNumber(UUID.randomUUID().toString());

        travelInfo.setTripId(String.valueOf(tripId));

        return travelInfo;
    }
}
