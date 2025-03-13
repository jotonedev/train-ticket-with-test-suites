package travel2.component.post;

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
import travel2.entity.TravelInfo;
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.entity.Type;
import travel2.repository.TripRepository;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateTripTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Trip already exists in database
     * <li><b>Parameters:</b></li> A Trip with an id that matches the id in the database
     * <li><b>Expected result:</b></li> status 0, msg "Trip already exists", no data
     * <li><b>Related Issue:</b></li> <b>F1a:</b> The logic of the implementation is correct, but the wrong status code
     * is returned anyway.
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateAlreadyExists() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TravelInfo travelInfo = configureTravelInfo();
        travelInfo.setTripId(storedTrip.getTripId().toString());

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Trip " + travelInfo.getTripId() + " already exists")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the database was unchanged
        assertEquals(1L, tripRepository.count());
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
    public void testCreateMultipleObjects() throws Exception {
        // Arrange
        TravelInfo[] info = {configureTravelInfo(), configureTravelInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Trip that does not already exist
     * <li><b>Parameters:</b></li> a Trip with some random id
     * <li><b>Expected result:</b></li> status 1, msg "Create trip info", no data. The Trip is stored to the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateCorrectObject() throws Exception {
        // Arrange
        TravelInfo travelInfo = configureTravelInfo();

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create trip info:" + travelInfo.getTripId() + ".")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the trip was saved to the database
        assertEquals(1L, tripRepository.count());
        TripId tripId = new TripId(travelInfo.getTripId());
        Optional<Trip> OptionalTrip = tripRepository.findById(tripId);
        assertTrue(OptionalTrip.isPresent());
        Trip trip = OptionalTrip.get();
        assertEquals(travelInfo.getTripId(), trip.getTripId().toString());
        assertEquals(travelInfo.getTrainTypeId(), trip.getTrainTypeId());
        assertEquals(travelInfo.getStartingStationId(), trip.getStartingStationId());
        assertEquals(travelInfo.getStationsId(), trip.getStationsId());
        assertEquals(travelInfo.getTerminalStationId(), trip.getTerminalStationId());
        assertEquals(travelInfo.getRouteId(), trip.getRouteId());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"some\":wrong, \"request\":json, \"string\":content}";

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
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
    void testCreateEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips")
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
    public void testCreateNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travel2service/trips")
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
