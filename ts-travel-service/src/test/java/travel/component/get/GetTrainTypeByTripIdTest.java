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
import travel.entity.TrainType;
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
public class GetTrainTypeByTripIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or there is no element in the database that
     * matches the id given in paths
     * <li><b>Parameters:</b></li> some random trip id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content", No data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTrainTypeByTripIdNotExist() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id of the trip
     * given in paths, and the mocked train-service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", TrainType
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTrainTypeByTripIdElementInDatabaseTrainServiceSuccess() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // For when trainservice returns a successful response
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", trainType), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(trainType.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Success", TrainType. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTrainTypeByTripIdElementInDatabaseTrainServiceSuccessMultipleId() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // For when trainservice returns a successful response
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", trainType), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", storedTrip.getTripId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(trainType.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id of the trip
     * given in paths, and the mocked train-service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a trip id that exists in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content", No data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTrainTypeByTripIdElementInDatabaseTrainServiceFailure() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        // For when trainservice returns an unsuccessful response
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by trainservice",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", storedTrip.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
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
    public void testGetTrainTypeByTripIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetTrainTypeByTripIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
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
