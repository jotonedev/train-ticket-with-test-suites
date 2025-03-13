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
import travel2.repository.TripRepository;

import java.util.List;

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
public class AdminQueryAllTripsTest {

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
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint correctly returns multiple object with the correct information.
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", data containing list of AdminTrips
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testAdminQueryAllValidObjects() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/travel2service/admin_trip"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Travel Service Admin Query All Travel Success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(7)))
                // check that trainType of array element matches initData
                .andExpect(jsonPath("$.data[0].trainType.id", is("ZhiDa")))
                // check that route of array element matches initData
                .andExpect(jsonPath("$.data[0].route.id", is("0b23bd3e-876a-4af3-b920-c50a90c90b04")))
                // check that trip of array element matches initData
                .andExpect(jsonPath("$.data[0].trip.tripId.type", is("Z")))
                .andExpect(jsonPath("$.data[0].trip.tripId.number", is("1234")))
                .andExpect(jsonPath("$.data[0].trip.startingStationId", is("shanghai")))
                .andExpect(jsonPath("$.data[0].trip.terminalStationId", is("beijing")))
                .andExpect(jsonPath("$.data[0].trip.startingTime", is("2013-05-04T01:51:52.000+00:00")))
                .andExpect(jsonPath("$.data[0].trip.endTime", is("2013-05-04T07:51:52.000+00:00")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint returns the correct response when there are no objects in the database.
     * <li><b>Parameters:</b></li> none, the database is however cleared
     * <li><b>Expected result:</b></li> status 0, msg "No Content", no data
     * </ul>
     *
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testAdminQueryAllZeroObjects() throws Exception {
        List<Trip> trips = tripRepository.findAll();

        try {
            tripRepository.deleteAll();

            // Act
            mockMvc.perform(get("/api/v1/travel2service/admin_trip"))
                    .andDo(print())
                    // Assert
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(0))
                    .andExpect(jsonPath("$.msg").value("No Content"))
                    .andExpect(jsonPath("$.data", nullValue()));

        } finally {
            // Restore database to its original state
            tripRepository.saveAll(trips);
        }
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
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
    @Order(3)
    public void testAdminQueryAllUnavailableService() throws Exception {
        // Arrange
        routeServiceContainer.stop();
        trainServiceContainer.stop();

        // Act
        mockMvc.perform(get("/api/v1/travel2service/admin_trip"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
