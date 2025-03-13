package travel.component.delete;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteTripTest {
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Trip that does exist in the database
     * <li><b>Parameters:</b></li> A Trip with an ID that matches the ID of a Trip in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete trip", the Deleted Trip id. The database no longer contains the Deleted Station.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteValidObject() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripId tripId = new TripId(storedTrip.getTripId().toString());

        // Act
        mockMvc.perform(delete("/api/v1/travelservice/trips/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + storedTrip.getTripId() + ".")))
                .andExpect(jsonPath("$.data", is(tripId.toString())));

        // Make sure the trip was deleted from the database
        assertEquals(0L, tripRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> A Trip with an ID that matches the ID of a Trip in the database and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Delete trip", the Deleted Trip id. The database no longer contains the Deleted Station.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteMultipleId() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripId tripId = new TripId(storedTrip.getTripId().toString());

        // Act
        mockMvc.perform(delete("/api/v1/travelservice/trips/{tripId}", storedTrip.getTripId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + storedTrip.getTripId() + ".")))
                .andExpect(jsonPath("$.data", is(tripId.toString())));

        // Make sure the trip was deleted from the database
        assertEquals(0L, tripRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Trip that does not already exists in the database
     * <li><b>Parameters:</b></li> a Trip with a random id
     * <li><b>Expected result:</b></li> status 0, msg "Trip doesn't exist", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteNotExists() throws Exception {
        // Arrange
        String tripId = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/travelservice/trips/{tripId}", tripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Trip " + tripId + " doesn't exist.")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the database was unchanged
        assertEquals(0L, tripRepository.count());
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
    public void testDeleteMissingId() throws Exception {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/travelservice/trips/{tripId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/travelservice/trips/{tripId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
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
