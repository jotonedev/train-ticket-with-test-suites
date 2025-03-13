package travel.component.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel.entity.Trip;
import travel.entity.TripId;
import travel.entity.Type;
import travel.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RetrieveTripTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when a Trip with the given id exists
     * <li><b>Parameters:</b></li> A Trip with a random id is stored in the database
     * <li><b>Expected result:</b></li> status 1, msg "Search Trip Success by Trip Id", the provided Trip is returned.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveExistingId() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/travelservice/trips/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Search Trip Success by Trip Id " + storedTrip.getTripId())))
                .andExpect(jsonPath("$.data.tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data.tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data.routeId", is(storedTrip.getRouteId())))
                .andExpect(jsonPath("$.data.trainTypeId", is(storedTrip.getTrainTypeId())))
                .andExpect(jsonPath("$.data.startingStationId", is(storedTrip.getStartingStationId())))
                .andExpect(jsonPath("$.data.stationsId", is(storedTrip.getStationsId())))
                .andExpect(jsonPath("$.data.terminalStationId", is(storedTrip.getTerminalStationId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> A Trip with a random id is stored in the database and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Search Trip Success by Trip Id", the provided Trip is returned. Only the first id is considered.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveMultipleId() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/travelservice/trips/{tripId}", storedTrip.getTripId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Search Trip Success by Trip Id " + storedTrip.getTripId())))
                .andExpect(jsonPath("$.data.tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data.tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data.routeId", is(storedTrip.getRouteId())))
                .andExpect(jsonPath("$.data.trainTypeId", is(storedTrip.getTrainTypeId())))
                .andExpect(jsonPath("$.data.startingStationId", is(storedTrip.getStartingStationId())))
                .andExpect(jsonPath("$.data.stationsId", is(storedTrip.getStationsId())))
                .andExpect(jsonPath("$.data.terminalStationId", is(storedTrip.getTerminalStationId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when a Trip with the given id does not exist
     * <li><b>Parameters:</b></li> A random id is provided
     * <li><b>Expected result:</b></li> status 0, msg "No Content according to tripId", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveMissingId() throws Exception {
        // Arrange
        String tripId = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/travelservice/trips/{tripId}", tripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content according to tripId" + tripId)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty path variable
     * <li><b>Expected result:</b></li> an IlleglArgumentException
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/travelservice/trips/{tripId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testRetrieveMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travelservice/trips/{tripId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
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
}
