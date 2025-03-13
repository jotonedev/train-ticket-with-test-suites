package travel2.component.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.entity.Type;
import travel2.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryAllTripsTest {

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
     * <li><b>Tests:</b></li> that all stored trips are returned on endpoint call
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all trips
     * @throws Exception
     */
    @Test
    public void testQueryAllElementInDatabase() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/travel2service/trips"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[0].tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[0].routeId", is(storedTrip.getRouteId())))
                .andExpect(jsonPath("$.data[0].trainTypeId", is(storedTrip.getTrainTypeId())))
                .andExpect(jsonPath("$.data[0].startingStationId", is(storedTrip.getStartingStationId())))
                .andExpect(jsonPath("$.data[0].stationsId", is(storedTrip.getStationsId())))
                .andExpect(jsonPath("$.data[0].terminalStationId", is(storedTrip.getTerminalStationId())));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No content", empty list of trips
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryAllEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travel2service/admin_trip"))
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
        tripId.setType(Type.Z);
        tripId.setNumber(UUID.randomUUID().toString());
        trip.setTripId(tripId);
        trip.setRouteId(UUID.randomUUID().toString());
        trip.setStartingTime(new Date());
        return tripRepository.save(trip);
    }
}
