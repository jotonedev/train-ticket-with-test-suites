package seat.integration.post;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import seat.entity.Seat;
import seat.entity.SeatClass;

import java.util.Date;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetLeftTicketOfIntervalTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Network network = Network.newNetwork();
    private final ObjectMapper mapper = new ObjectMapper();
    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.travel.service.url", travelServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelServiceContainer.getMappedPort(12346));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves with the trainNumber starting with G
     * <li><b>Parameters:</b></li> a valid request object with the trainNumber starting with G
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", 1073741823 (num of left tickets). Because
     * the trainNumber starts with G, the requests are sent to ts-travel-service and ts-order-service.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    void testGetLeftTicketOfIntervalTrainNumberG() throws Exception {
        // Arrange
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("nanjing");
        seat.setDestStation("shanghai");
        seat.setTrainNumber("G1236");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves with the trainNumber not starting with G or D
     * <li><b>Parameters:</b></li> a valid request object with the trainNumber starting with G or D
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", 1073741823 (num of left tickets). Because
     * the trainNumber starts with G, the requests are sent to ts-travel2-service and ts-order-other-service.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    void testGetLeftTicketOfIntervalTrainNumberNotGOrD() throws Exception {
        // Arrange
        Seat seat = new Seat();
        seat.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("shanghai");
        seat.setDestStation("beijing");
        seat.setTrainNumber("K1345");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when startStation and destStation do not exist
     * <li><b>Parameters:</b></li> a valid request object with startStation and destStation not existing
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", 1073741823 (num of left tickets). startStation and destStation
     * are not relevant in sending a request and the attributes are just saved into the ticket object as they exist in the seat object.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    void testGetLeftTicketOfIntervalStartStationDestStationNonExisting() throws Exception {
        // Arrange
        Seat seatRequest = new Seat();
        seatRequest.setTravelDate(new Date());
        seatRequest.setTrainNumber("K1345");
        seatRequest.setSeatType(SeatClass.BUSINESS.getCode());
        seatRequest.setStartStation("notExisting");
        seatRequest.setDestStation("notExisting");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seatRequest);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trainNumber does not exist
     * <li><b>Parameters:</b></li> a valid request object with the trainNumber not existing
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    void testGetLeftTicketOfIntervalTrainNumberNonExisting() throws Exception {
        // Assert
        Seat seatRequest = new Seat();
        seatRequest.setTravelDate(new Date());
        seatRequest.setTrainNumber("K0000");
        seatRequest.setSeatType(SeatClass.BUSINESS.getCode());
        seatRequest.setStartStation("shanghai");
        seatRequest.setDestStation("beijing");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seatRequest);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is out of range
     * <li><b>Parameters:</b></li> a valid request object with a seatType of 0
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", 1073741823 (num of left tickets). The seatType is assumed to be 3
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    void testGetLeftTicketOfIntervalSeatTypeOutOfRange() throws Exception {
        // Arrange
        Seat seat = new Seat();
        seat.setTrainNumber("K1345");
        seat.setSeatType(9); // Does not exist
        seat.setTravelDate(new Date("Mon May 04 09:51:52 GMT+0800 2013"));
        seat.setStartStation("shanghai");
        seat.setDestStation("beijing");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-seat-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    void testGetLeftTicketOfIntervalServiceUnavailable() throws Exception {
        // Arrange
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        travelServiceContainer.stop();
        travel2ServiceContainer.stop();
        routeServiceContainer.stop();
        trainServiceContainer.stop();

        Seat seat = new Seat();
        seat.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        seat.setSeatType(SeatClass.FIRSTCLASS.getCode());
        seat.setStartStation("nanjing");
        seat.setDestStation("shanghai");
        seat.setTrainNumber("G1236");

        //Actual request to the endpoint we want to test
        String jsonString = mapper.writeValueAsString(seat);

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
