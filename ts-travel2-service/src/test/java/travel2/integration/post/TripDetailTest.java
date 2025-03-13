package travel2.integration.post;

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
import travel2.entity.Trip;
import travel2.entity.TripAllDetailInfo;
import travel2.entity.TripId;
import travel2.entity.Type;
import travel2.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TripDetailTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    private static final MongoDBContainer travel2MongoDBContainer = new MongoDBContainer(
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
            .dependsOn(travel2MongoDBContainer);

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
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripAllDetailInfo with valid data where the seat-service
     * calculates the ticket number with finding orders
     * <li><b>Parameters:</b></li> TripAllDetailInfo chosen in a way such that all called services are able to utilize data from their
     * init files, including the order-other-service, which fill find order. This is because the combination of the station names
     * "Shang Hai" and "Tai Yuan" leads to an order with trainNumber "Z1234", which exists in the initData of the order-other-service
     * with the corresponding travelDate
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TripAllDetail object. The response is calculated
     * with finding orders
     * <li><b>Related Issue:</b></li> <b>F17:</b> {@code Integer.parseInt(tempOrder.getSeatNumber())} will cause a
     * NumberFormatException in the order-service when running the service in a container, because all SeatNumbers within the
     * initData of the order services are "FirstClass-30", which cannot be parsed to a String. Changing the SeatNumber from
     * "A6" to "2" in the initData of the order-other-service will result in a passing test.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void FAILING_testGetTripAllDetailInfoFullyValidRequestBodyOrderFound() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("Z1234", "Shang Hai", "Tai Yuan");

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.type", is("Z")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.number", is("1234")))
                .andExpect(jsonPath("$.data.tripResponse.trainTypeId", is("ZhiDa")))
                .andExpect(jsonPath("$.data.tripResponse.startingStation", is("Shang Hai")))
                .andExpect(jsonPath("$.data.tripResponse.terminalStation", is("Tai Yuan")))
                .andExpect(jsonPath("$.data.tripResponse.startingTime", is("2013-05-04T01:51:52.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.endTime", is("2013-05-04T12:41:52.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.priceForEconomyClass", is("454.99999999999994")))
                .andExpect(jsonPath("$.data.tripResponse.priceForConfortClass", is("1300.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripAllDetailInfo with valid data
     * <li><b>Parameters:</b></li> TripAllDetailInfo chosen in a way such that all called services are able to utilize data from their
     * init files
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TripAllDetail object
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetTripAllDetailInfoFullyValidRequestBodyNoOrderFound() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("Z1235", "Nan Jing", "Bei Jing");

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.type", is("Z")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.number", is("1235")))
                .andExpect(jsonPath("$.data.tripResponse.trainTypeId", is("ZhiDa")))
                .andExpect(jsonPath("$.data.tripResponse.startingStation", is("Nan Jing")))
                .andExpect(jsonPath("$.data.tripResponse.terminalStation", is("Bei Jing")))
                .andExpect(jsonPath("$.data.tripResponse.startingTime", is("2013-05-04T03:31:52.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.endTime", is("2013-05-04T13:31:52.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.priceForEconomyClass", is("420.0")))
                .andExpect(jsonPath("$.data.tripResponse.priceForConfortClass", is("1200.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId does not exist
     * <li><b>Parameters:</b></li> TripAllDetailInfo configured such that its tripId does not exist in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetail with empty valyes
     * </ul>
     * @throws Exception
     */
    @Order(3)
    @Test
    public void testGetTripAllDetailInfoMissingTrip() throws Exception {
        // Arrange
        TripAllDetailInfo info = new TripAllDetailInfo();
        info.setTripId("non-existing-trip-id");

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse", nullValue()))
                .andExpect(jsonPath("$.data.trip", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when routeId is not found in the database
     * <li><b>Parameters:</b></li> TripAllDetailInfo chosen in a way such that all called services are able to utilize data from their
     * init files, except for the routeservice, which is unable to find a route
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testGetTripAllDetailInfoRouteNotExists() throws Exception {
        // Arrange
        // get the trip from database and alter the routeId in such a way that the routeservice is unable to find a route
        Trip trip = getTripByNumber("1235");
        trip.setRouteId(UUID.randomUUID().toString());
        tripRepository.save(trip);

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("Z1235", "Shang Hai", "Tai Yuan");

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body. However, startingPlace and / or endPlace are empty Strings
     * <li><b>Parameters:</b></li> a TripAllDetailInfo entity with a trip id that exists in the database, but the startingPlace and
     * endPlace are empty Strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetail with empty valyes. The service should not be
     * able to configure a proper TripAllDetail object, so the response should be similar to when the tripId does not exist in the database.
     * <li><b>Related Issue:</b></li> <b>F13c:</b> A request to the wrong endpoint is made. The service tries to call
     * {@code GET api/v1/ticketinfoservice/ticketinfo/{name}}, but because name is empty and not checked for, the service will call
     * {@code GET api/v1/ticketinfoservice/ticketinfo/}, which does exist as well, but only as the HTTP Method POST. Therefore,
     * the service will return a 405 Method Not Allowed error, which is unhandled by the service.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void FAILING_testGetTripAllDetailInfoFromToEmptyStrings() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("Z1235", "Nan Jing", "Bei Jing");
        tripAllDetailInfo.setFrom("");
        tripAllDetailInfo.setTo("");

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse", nullValue()))
                .andExpect(jsonPath("$.data.trip", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-travel2-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel2-service</li>
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
    @Order(6)
    public void testAdminQueryAllUnavailableService() throws Exception {
        // Arrange
        routeServiceContainer.stop();
        trainServiceContainer.stop();
        seatServiceContainer.stop();
        ticketinfoServiceContainer.stop();
        orderOtherServiceContainer.stop();
        stationServiceContainer.stop();
        basicServiceContainer.stop();
        configServiceContainer.stop();
        travel2ServiceContainer.stop();
        priceServiceContainer.stop();

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("Z1235", "Shang Hai", "Tai Yuan");

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private TripAllDetailInfo configureTripAllDetailInfo(String tripId, String from, String to) {
        TripAllDetailInfo tripAllDetailInfo = new TripAllDetailInfo();
        tripAllDetailInfo.setTripId(tripId);
        tripAllDetailInfo.setFrom(from);
        tripAllDetailInfo.setTo(to);
        tripAllDetailInfo.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 3025"));

        return tripAllDetailInfo;
    }

    private Trip getTripByNumber(String tripName) {
        TripId tripId = new TripId();

        // configure a tripId object for "Z[tripName]"
        tripId.setType(Type.Z);
        tripId.setNumber(tripName);

        return tripRepository.findByTripId(tripId);
    }
}
