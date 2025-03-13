package travel.integration.post;

import org.junit.jupiter.api.*;
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
import travel.entity.*;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripsLeftTest {

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    private static final MongoDBContainer travelMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");
    @Container
    private static final GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelMongoDBContainer);

    @Container
    private static final MongoDBContainer configMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    private static final GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configMongoDBContainer);

    @Container
    private static final GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @Container
    private static final GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");

    @Container
    private static final GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
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
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    private static final GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    private static final GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    public static MongoDBContainer priceServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");

    @Container
    public static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDbContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.ticketinfo.service.url", ticketinfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketinfoServiceContainer.getMappedPort(15681));
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
        registry.add("ts.travel.service.url", travelServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelServiceContainer.getMappedPort(12346));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripInfo with valid startingPlace and endPlace names where
     * the seat-service calculates the ticket number with finding orders
     * <li><b>Parameters:</b></li> chosen in a way such that all called services are able to utilize data from their
     * init files, including the order-service, which will find orders. This is because the combination of the
     * Station names "Nan Jing" and "Shang Hai" leads to an order with trainNumber "G1234", which exists in the
     * initData of the order-service with the corresponding travelDate.
     * <li><b>Expected result:</b></li> status 1, msg "Success", data containing a list of TripResponses. The response is
     * calculated with finding orders.
     * <li><b>Related Issue:</b></li> <b>F17:</b> {@code Integer.parseInt(tempOrder.getSeatNumber())} will cause a
     * NumberFormatException in the order-service when running the service in a container, because all SeatNumbers within the
     * initData of the order services are "FirstClass-30", which cannot be parsed to a String. Changing the SeatNumber from
     * "FirstClass-30" to "2" in the initData of the order-service will result in a passing test.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void FAILING_testQueryFullyValidRequestBodyOrderFound() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo("Nan Jing", "Shang Hai");
        System.out.println(tripInfo.getDepartureTime());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].tripId.type", is("G")))
                .andExpect(jsonPath("$.data[0].tripId.number", is("1234")))
                .andExpect(jsonPath("$.data[0].trainTypeId", is("GaoTieOne")))
                .andExpect(jsonPath("$.data[0].startingStation", is("Nan Jing")))
                .andExpect(jsonPath("$.data[0].terminalStation", is("Shang Hai")))
                .andExpect(jsonPath("$.data[0].startingTime", is("2013-05-04T01:00:00.000+00:00")))
                .andExpect(jsonPath("$.data[0].endTime", is("2013-05-04T02:00:00.000+00:00")))
                .andExpect(jsonPath("$.data[0].economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].priceForEconomyClass", is("95.0")))
                .andExpect(jsonPath("$.data[0].priceForConfortClass", is("250.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripInfo with valid startingPlace and endPlace names where
     * the seat-service calculates the ticket number without finding any orders
     * <li><b>Parameters:</b></li> chosen in a way such that all called services are able to utilize data from their
     * init files, apart from the order-service, which will not find any orders. This is because the combination of the
     * Station names "Shang Hai" and "Su Zhou" leads to an order with trainNumber "D1345", which does not exist in the
     * initData of the order-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success", data containing a list of TripResponses. The response is
     * calculated without finding any orders.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testQueryFullyValidRequestBodyNoOrderFound() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo("Shang Hai", "Su Zhou");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId.type", is("D")))
                .andExpect(jsonPath("$.data[0].tripId.number", is("1345")))
                .andExpect(jsonPath("$.data[0].trainTypeId", is("DongCheOne")))
                .andExpect(jsonPath("$.data[0].startingStation", is("Shang Hai")))
                .andExpect(jsonPath("$.data[0].terminalStation", is("Su Zhou")))
                .andExpect(jsonPath("$.data[0].startingTime", is("2013-05-03T23:00:00.000+00:00")))
                .andExpect(jsonPath("$.data[0].endTime", is("2013-05-03T23:16:00.000+00:00")))
                .andExpect(jsonPath("$.data[0].economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].priceForEconomyClass", is("22.5")))
                .andExpect(jsonPath("$.data[0].priceForConfortClass", is("50.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the startingPlace and / or endPlace names cannot be found
     * <li><b>Parameters:</b></li> chosen in a way such that the startingPlace and / or endPlace names cannot be found
     * by the ticketinfoservice
     * <li><b>Expected result:</b></li> status 1, msg "Success", an empty list of TripResponse
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testQueryStartingAndEndPlaceNotExist() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
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
    @Order(4)
    public void testAdminQueryAllUnavailableService() throws Exception {
        // Arrange
        routeServiceContainer.stop();
        trainServiceContainer.stop();
        seatServiceContainer.stop();
        ticketinfoServiceContainer.stop();
        orderServiceContainer.stop();
        stationServiceContainer.stop();
        basicServiceContainer.stop();
        configServiceContainer.stop();
        travelServiceContainer.stop();
        priceServiceContainer.stop();

        TripInfo tripInfo = configureTripInfo("Nan Jing", "Shang Hai");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trips/left")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private TripInfo configureTripInfo(String startingPlace, String endPLace) {
        TripInfo tripInfo = new TripInfo();
        tripInfo.setStartingPlace(startingPlace);
        tripInfo.setEndPlace(endPLace);
        tripInfo.setDepartureTime(new Date("Sat Jul 29 00:00:00 GMT+0800 3025"));

        return tripInfo;
    }
}
