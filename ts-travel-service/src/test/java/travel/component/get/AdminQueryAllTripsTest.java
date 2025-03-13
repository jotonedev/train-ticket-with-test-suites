package travel.component.get;

import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel.entity.*;
import travel.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminQueryAllTripsTest {
    @Autowired
    private TripRepository tripRepository;

    @MockBean
    private RestTemplate restTemplate;

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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all trips
     * @throws Exception
     */
    @Test
    public void testQueryAllElementInDatabase() throws Exception {
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        Route route = new Route();
        route.setId(storedTrip.getRouteId());
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", route), HttpStatus.OK));

        TrainType trainType = new TrainType();
        trainType.setId(storedTrip.getTrainTypeId());
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", trainType), HttpStatus.OK));

        mockMvc.perform(get("/api/v1/travelservice/admin_trip"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))

                // check that trip data was assigned correctly
                .andExpect(jsonPath("$.data[0].trip.tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data[0].trip.tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data[0].trip.startingStationId", is(storedTrip.getStartingStationId())))
                .andExpect(jsonPath("$.data[0].trip.stationsId", is(storedTrip.getStationsId())))
                .andExpect(jsonPath("$.data[0].trip.terminalStationId", is(storedTrip.getTerminalStationId())))
                .andExpect(jsonPath("$.data[0].trip.routeId", is(storedTrip.getRouteId())))
                .andExpect(jsonPath("$.data[0].trip.trainTypeId", is(storedTrip.getTrainTypeId())))

                // mocked values
                .andExpect(jsonPath("$.data[0].route.id", is(route.getId())))
                .andExpect(jsonPath("$.data[0].trainType.id", is(trainType.getId())));
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
        mockMvc.perform(get("/api/v1/travelservice/admin_trip"))
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
}
