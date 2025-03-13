package travel2.component.get;

import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
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
import travel2.entity.Route;
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.entity.Type;
import travel2.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
public class GetRouteByTripIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId given in paths is only one character long
     * <li><b>Parameters:</b></li> a tripId that is only one character long
     * <li><b>Expected result:</b></li> status 0, msg "No Content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdTripIdTooShort() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // make tripId too short
        storedTrip.getTripId().setNumber("");

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("\"[Get Route By Trip ID] Trip Not Found:\" + tripId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or there is no element in the database that matches
     * the id given in paths
     * <li><b>Parameters:</b></li> some random trip id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content", No data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdNotExist() throws Exception {
        // Arrange
        String tripId = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}", tripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("\"[Get Route By Trip ID] Trip Not Found:\" + tripId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the The database contains an element that matches the id of the
     * trip given in paths, and the mocked route-service succeeds on call
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", Route
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdElementInDatabaseRouteServiceSuccess() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        Route route = new Route();
        route.setId(UUID.randomUUID().toString());

        // For when routeservice returns a successful response
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", route), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Get Route By Trip ID] Success")))
                .andExpect(jsonPath("$.data.id", is(route.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database and an additional random id
     * <li><b>Expected result:</b></li> status 1, msg "Success", Route. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdElementInDatabaseRouteServiceSuccessMultipleId() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        Route route = new Route();
        route.setId(UUID.randomUUID().toString());

        // For when routeservice returns a successful response
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", route), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}", storedTrip.getTripId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Get Route By Trip ID] Success")))
                .andExpect(jsonPath("$.data.id", is(route.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the The database contains an element that matches the id of the
     * trip given in paths, and the mocked route-service fails on call
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database
     * <li><b>Expected result:</b></li> status 0, msg "Route not found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdElementInDatabaseRouteServiceFailure() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        Route route = new Route();
        route.setId(UUID.randomUUID().toString());

        // For when routeservice returns an unsuccessful response
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by routeservice",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("\"[Get Route By Trip ID] Route Not Found:\" + trip.getRouteId()")))
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
    public void testGetRouteByTripIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetRouteByTripIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{tripId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
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
