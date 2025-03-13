package travelplan.integration.post;

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
import travelplan.entity.TripInfo;

import java.util.Date;
import java.util.UUID;

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
public class GetMinStationTest {

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    private static GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);


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

    @Container
    private static final GenericContainer<?> routePlanContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-plan-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14578)
            .withNetwork(network)
            .withNetworkAliases("ts-route-plan-service");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.route.plan.service.url", routePlanContainer::getHost);
        registry.add("ts.route.plan.service.port", () -> routePlanContainer.getMappedPort(14578));
    }

    /**
     * <ul>
     * <li><b>called by ts-travel-plan-service:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is fully valid and the seat-service calculates
     * the ticket number with finding orders.
     * <li><b>Parameters:</b></li> a TripInfo object with valid station names that can be found in the init files of the
     * other services, including order services, which will find orders. This is because the
     * combination of "Shang Hai" and "Tai Yuan" leads to an order with trainNumber "Z1234", which exists in the initData
     * of the order service with the corresponding travelDate.
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a list of TravelAdvanceResultUnits with the correct data.
     * The response is calculated with finding orders.
     * <li><b>Related Issue:</b></li> <b>F17:</b> {@code Integer.parseInt(tempOrder.getSeatNumber())} will cause a
     * NumberFormatException in the order-service when running the service in a container, because all SeatNumbers within the
     * initData of the order services are "FirstClass-30", which cannot be parsed to a String. Changing the SeatNumber from
     * "FirstClass-30" to "2" in the initData of the order-service will result in a passing test.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void FAILING_testGetMinStationFullyValidRequestBody() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo("Shang Hai", "Tai Yuan");

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId", is("Z1234")))
                .andExpect(jsonPath("$.data[0].trainTypeId", is("ZhiDa")))
                .andExpect(jsonPath("$.data[0].fromStationName", is("Shang Hai")))
                .andExpect(jsonPath("$.data[0].toStationName", is("Tai Yuan")))
                .andExpect(jsonPath("$.data[0].stopStations").isArray())
                .andExpect(jsonPath("$.data[0].stopStations", hasSize(4)))
                .andExpect(jsonPath("$.data[0].stopStations[0]", is("Shang Hai")))
                .andExpect(jsonPath("$.data[0].stopStations[1]", is("Nan Jing")))
                .andExpect(jsonPath("$.data[0].stopStations[2]", is("Shi Jia Zhuang")))
                .andExpect(jsonPath("$.data[0].stopStations[3]", is("Tai Yuan")))
                .andExpect(jsonPath("$.data[0].priceForSecondClassSeat", is("454.99999999999994")))
                .andExpect(jsonPath("$.data[0].priceForFirstClassSeat", is("1300.0")))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketSecondClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketFirstClass", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>called by ts-travel-plan-service:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is fully valid and the seat-service calculates the
     * ticket number without finding any orders.
     * <li><b>Parameters:</b></li> a TripInfo object with valid station names that can be found in the init files of the
     * other services., apart from the order services, which will not find any orders. This is because the combination of the
     * Station names "Nan Jing" and "Bei Jing" lead to an order with trainNumber "Z1235", which does not exist in the initData
     * with the corresponding travelDate
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a list of TravelAdvanceResultUnits with the correct data.
     * The response is calculated without finding any orders.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetQuickestFullyValidRequestBody() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo("Nan Jing", "Bei Jing");

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId", is("Z1235")))
                .andExpect(jsonPath("$.data[0].trainTypeId", is("ZhiDa")))
                .andExpect(jsonPath("$.data[0].fromStationName", is("Nan Jing")))
                .andExpect(jsonPath("$.data[0].toStationName", is("Bei Jing")))
                .andExpect(jsonPath("$.data[0].stopStations").isArray())
                .andExpect(jsonPath("$.data[0].stopStations", hasSize(4)))
                .andExpect(jsonPath("$.data[0].stopStations[0]", is("Nan Jing")))
                .andExpect(jsonPath("$.data[0].stopStations[1]", is("Xu Zhou")))
                .andExpect(jsonPath("$.data[0].stopStations[2]", is("Ji Nan")))
                .andExpect(jsonPath("$.data[0].stopStations[3]", is("Bei Jing")))
                .andExpect(jsonPath("$.data[0].priceForSecondClassSeat", is("420.0")))
                .andExpect(jsonPath("$.data[0].priceForFirstClassSeat", is("1200.0")))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketSecondClass", is(1073741823)))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketFirstClass", is(1073741823)));
    }

    /**
     * <ul>
     * <li><b>called by ts-travel-plan-service:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the startingPlace and / or endPlace do not exist for the travelservice
     * and travel2service and cannot be found in the init files of the other services.
     * <li><b>Parameters:</b></li> a TripInfo object with random station names.
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> when startId and terminalId are configured such that the route service
     * is unable to find routes with the startId and terminalId, the data of the response is null, which will trigger a NullPointer
     * when trying to access some info later. As the fallback response is triggered with a 200 OK, it remains unclear whether
     * developers deliberately omitted preventative measures (e.g. null-checks) in favor of relying on the
     * fallback (which would be considered intended behavior), or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testGetMinStationNoStationsExist() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>called by ts-travel-plan-service:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travelDate of TripInfo is from the past.
     * <li><b>Parameters:</b></li> a TripInfo object with a travelDate from the past.
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * This is because the travel services will crash when the date is from the past
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testGetMinStationDepartureTimeFromPast() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo("Shang Hai", "Tai Yuan");

        Date dateFromPast = new Date(0); // 1970-01-01 00:00:00
        tripInfo.setDepartureTime(dateFromPast);

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/minStation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>called by ts-travel-plan-service:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
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
    @Order(5)
    public void testGetMinStationUnavailableService() throws Exception {
        // Arrange
        orderOtherServiceContainer.stop();
        travelServiceContainer.stop();
        seatServiceContainer.stop();
        orderServiceContainer.stop();
        stationServiceContainer.stop();
        travel2ServiceContainer.stop();
        ticketinfoServiceContainer.stop();
        basicServiceContainer.stop();
        trainServiceContainer.stop();
        routeServiceContainer.stop();
        priceServiceContainer.stop();
        configServiceContainer.stop();
        routePlanContainer.stop();

        TripInfo tripInfo = configureTripInfo("Shang Hai", "Tai Yuan");

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/minStation")
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
        tripInfo.setDepartureTime(new Date());

        return tripInfo;
    }
}
