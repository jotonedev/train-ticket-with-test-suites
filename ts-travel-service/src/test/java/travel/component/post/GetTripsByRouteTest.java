package travel.component.post;

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
import travel.entity.TravelInfo;
import travel.entity.Trip;
import travel.entity.TripId;
import travel.entity.Type;
import travel.repository.TripRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetTripsByRouteTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or there is no trip in the database that associates
     * the route id of any element within the list of ids in the request body
     * <li><b>Parameters:</b></li> a list containing some random route id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content", no data.
     * <li><b>Related Issue:</b></li> <b>F4d:</b> The service performs {@code {repository.findByRouteId(routeId);}}
     * which returns a representation of a Collection, but is compared with null instead of isEmpty(). This will lead to the
     * endpoint succeeding instead of actually failing
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetTripByRouteNoMatchingElementInDatabase() throws Exception {
        // Arrange
        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeIds)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains all trips that associate each route id of any element
     * within the list of ids in the request body
     * <li><b>Parameters:</b></li> a list containing the route id of a trip that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a created list of trip lists for the given route ids.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripByRouteAllMatchingElementsInDatabase() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add(storedTrip.getRouteId());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeIds)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0][0].tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[0][0].tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[0][0].routeId", is(storedTrip.getRouteId())));
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
    public void testGetTripByRouteMultipleObjects() throws Exception {
        // Arrange
        TravelInfo[] info = {configureTravelInfo(), configureTravelInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
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
    public void testGetTripByRouteMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"some\":wrong, \"request\":json, \"string\":content}";

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
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
    void testGetTripByRouteEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
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
    public void testGetTripByRouteNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the list of route ids provided in the request body contains both ids that
     * associate to a trip in the database and ids which don’t
     * <li><b>Parameters:</b></li> a list containing the route id of a trip that exists in the database and a random route id that does
     * not exist in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a created list of trip lists for the given route ids
     * <li><b>Related Issue:</b></li> <b>F4d:</b> The service performs {@code {repository.findByRouteId(routeId);}}
     * which returns a representation of a Collection, but is compared with null instead of isEmpty(). This will lead to the
     * endpoint succeeding instead of actually failing
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetTripByRouteMixedMatchingElementsInDatabase() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add(UUID.randomUUID().toString());
        routeIds.add(storedTrip.getRouteId());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeIds)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0][0].tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[0][0].tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[0][0].routeId", is(storedTrip.getRouteId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the list of route ids provided in the request body contains duplicates
     * that associate to trips within the database
     * <li><b>Parameters:</b></li> a list of route ids that contains duplicate ids that associate to a trip in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a created list of trip lists where two or more lists are equal as
     * they refer to the same routeId.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripByRouteDuplicateRouteIdsInList() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        ArrayList<String> routeIds = new ArrayList<>();
        routeIds.add(storedTrip.getRouteId());
        routeIds.add(storedTrip.getRouteId());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeIds)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0][0].tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[0][0].tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[0][0].routeId", is(storedTrip.getRouteId())))
                .andExpect(jsonPath("$.data[1][0].tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[1][0].tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[1][0].routeId", is(storedTrip.getRouteId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the list of route ids provided in the request body contains duplicates
     * that associate to trips within the database
     * <li><b>Parameters:</b></li> an empty list
     * <li><b>Expected result:</b></li> status 0, msg "No content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripByRouteEmptyList() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new ArrayList<>())))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Trip populateDatabase() {
        Trip trip = new Trip();
        TripId tripId = new TripId();
        tripId.setType(Type.G);
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
        tripId.setType(Type.G);
        tripId.setNumber(UUID.randomUUID().toString());

        travelInfo.setTripId(String.valueOf(tripId));

        return travelInfo;
    }
}
