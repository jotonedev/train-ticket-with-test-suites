package travel.integration.get;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import travel.entity.Trip;
import travel.entity.TripId;
import travel.entity.Type;
import travel.repository.TripRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetTrainTypeByTripIdTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");
    @Container
    private static final GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving the train type correctly handles the case
     * when the request is valid. It ensures that the endpoint returns a response with the appropriate content.
     * <li><b>Parameters:</b></li> A tripId that exists in the database.
     * <li><b>Expected result:</b></li> status 1, msg "Success", data containing the trainType found by the trainservice
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testGetTrainTypeByTripIdValidTripId() throws Exception {
        // Arrange
        String existingTripId = "G1234";

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", existingTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is("GaoTieOne")))
                .andExpect(jsonPath("$.data.economyClass", is(Integer.MAX_VALUE)))
                .andExpect(jsonPath("$.data.confortClass", is(Integer.MAX_VALUE)))
                .andExpect(jsonPath("$.data.averageSpeed", is(250)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving the train type correctly handles the case
     * when the request is valid, but the tripId does not exist in the tripRepository.
     * <li><b>Parameters:</b></li> tripId chosen in a way such that the travelservice is able to utilize data from its
     * init files, but the trainservice is unable to find a trainType
     * <li><b>Expected result:</b></li> status 0, msg "No Content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetTrainTypeByTripIdTrainTypeNotExists() throws Exception {
        // Arrange
        String existingTripId = "G1235";

        // get the trip from database and alter the trainTypeId in such a way that the trainservice is unable to find
        // a trainType
        Trip trip = getTripByNumber("1235");
        trip.setTrainTypeId(UUID.randomUUID().toString());
        tripRepository.save(trip);

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", existingTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testAdminQueryAllUnavailableService() throws Exception {
        // Arrange
        trainServiceContainer.stop();
        String existingTripId = "G1235";

        // Act
        mockMvc.perform(get("/api/v1/travelservice/train_types/{tripId}", existingTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Trip getTripByNumber(String tripName) {
        TripId tripId = new TripId();

        // configure a tripId object for "Z[tripName]"
        tripId.setType(Type.G);
        tripId.setNumber(tripName);

        return tripRepository.findByTripId(tripId);
    }
}
