package travel2.integration.get;

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
import travel2.entity.Trip;
import travel2.entity.TripId;
import travel2.entity.Type;
import travel2.repository.TripRepository;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.*;
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
public class GetRouteByTripIdTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");
    @Container
    private static final GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178).toString());
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving the route correctly handles the case when
     * the request is valid and checks the response data.
     * <li><b>Parameters:</b></li> a tripId that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", data containing the route
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testGetRouteByTripIdValidTripId() throws Exception {
        // Arrange
        String existingTripId = "Z1234";

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{routeId}", existingTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Get Route By Trip ID] Success")))
                .andExpect(jsonPath("$.data.id", is("0b23bd3e-876a-4af3-b920-c50a90c90b04")))
                .andExpect(jsonPath("$.data.startStationId", is("shanghai")))
                .andExpect(jsonPath("$.data.terminalStationId", is("taiyuan")))
                // check station array
                .andExpect(jsonPath("$.data.stations").isArray())
                .andExpect(jsonPath("$.data.stations", hasSize(4)))
                .andExpect(jsonPath("$.data.stations[0]", is("shanghai")))
                .andExpect(jsonPath("$.data.stations[1]", is("nanjing")))
                .andExpect(jsonPath("$.data.stations[2]", is("shijiazhuang")))
                .andExpect(jsonPath("$.data.stations[3]", is("taiyuan")))
                // check distance array
                .andExpect(jsonPath("$.data.distances").isArray())
                .andExpect(jsonPath("$.data.distances", hasSize(4)))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(350)))
                .andExpect(jsonPath("$.data.distances[2]", is(1000)))
                .andExpect(jsonPath("$.data.distances[3]", is(1300)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving the route correctly handles the case when
     * there is no route that matches the trip.
     * <li><b>Parameters:</b></li> a tripId that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Trip Not Found", no data
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetRouteByTripIdWrongId() throws Exception {
        // Arrange
        String tripId = "no-existing";

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{routeId}", tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("\"[Get Route By Trip ID] Trip Not Found:\" + tripId")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving the route correctly handles the case when
     * the request is valid, but there are no routes in the database.
     * <li><b>Parameters:</b></li> tripId chosen in a way such that the travel2service is able to utilize data from its
     * init files, but the routeservice is unable to find a route
     * <li><b>Expected result:</b></li> status 0, msg "Trip Not Found", no data
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testGetRouteByTripIdRouteNotExists() throws Exception {
        // Arrange
        String existingTripId = "G1235";

        // get the trip from database and alter the routeId in such a way that the routeservice is unable to find
        // a route
        Trip trip = getTripByNumber("1235");
        trip.setRouteId(UUID.randomUUID().toString());
        tripRepository.save(trip);

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{routeId}", existingTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("\"[Get Route By Trip ID] Trip Not Found:\" + tripId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testAdminQueryAllUnavailableService() throws Exception {
        // Arrange
        routeServiceContainer.stop();
        String existingTripId = "Z1234";

        // Act
        mockMvc.perform(get("/api/v1/travel2service/routes/{routeId}", existingTripId))
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
        tripId.setType(Type.Z);
        tripId.setNumber(tripName);

        return tripRepository.findByTripId(tripId);
    }
}
