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
import travel.repository.TripRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
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
public class TripDetailTest {

    @Autowired
    private TripRepository tripRepository;

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripAllDetailInfo with valid data where the seat-service
     * calculates the ticket number with finding orders
     * <li><b>Parameters:</b></li> TripAllDetailInfo chosen in a way such that all called services are able to utilize data from their
     * init files, including the order-service, which will find orders. This is because the combination of the
     * Station names "Nan Jing" and "Shang Hai" leads to an order with trainNumber "G1234", which does not exist in the
     * initData of the order-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TripAllDetail object. THe response is calculated with
     * finding orders.
     * <li><b>Related Issue:</b></li> <b>F17:</b> {@code Integer.parseInt(tempOrder.getSeatNumber())} will cause a
     * NumberFormatException in the order-service when running the service in a container, because all SeatNumbers within the
     * initData of the order services are "FirstClass-30", which cannot be parsed to a String. Changing the SeatNumber from
     * "FirstClass-30" to "2" in the initData of the order-service will result in a passing test.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void FAILING_testGetTripAllDetailInfoFullyValidRequestBodyOrderFound() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("G1234", "Nan Jing", "Shang Hai");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.type", is("G")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.number", is("1234")))
                .andExpect(jsonPath("$.data.tripResponse.trainTypeId", is("GaoTieOne")))
                .andExpect(jsonPath("$.data.tripResponse.startingStation", is("Nan Jing")))
                .andExpect(jsonPath("$.data.tripResponse.terminalStation", is("Shang Hai")))
                .andExpect(jsonPath("$.data.tripResponse.startingTime", is("2013-05-04T01:00:00.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.endTime", is("2013-05-04T02:00:00.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.priceForEconomyClass", is("95.0")))
                .andExpect(jsonPath("$.data.tripResponse.priceForConfortClass", is("250.0")));
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a TripAllDetailInfo with valid data where the seat-service
     * calculates the ticket number without finding orders
     * <li><b>Parameters:</b></li> TripAllDetailInfo chosen in a way such that all called services are able to utilize data from their
     * init files, apart from the order-service, which will not find any orders. This is because the combination of the Station
     * names "Shang Hai" and "Su Zhou" leads to an order with trainNumber "D1345", which does not exist in the initData of the
     * order-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TripAllDetail object. The response is calculated
     * without finding any orders.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetTripAllDetailInfoFullyValidRequestBodyNoOrderFound() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("D1345", "Shang Hai", "Su Zhou");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.type", is("D")))
                .andExpect(jsonPath("$.data.tripResponse.tripId.number", is("1345")))
                .andExpect(jsonPath("$.data.tripResponse.trainTypeId", is("DongCheOne")))
                .andExpect(jsonPath("$.data.tripResponse.startingStation", is("Shang Hai")))
                .andExpect(jsonPath("$.data.tripResponse.terminalStation", is("Su Zhou")))
                .andExpect(jsonPath("$.data.tripResponse.startingTime", is("2013-05-03T23:00:00.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.endTime", is("2013-05-03T23:16:00.000+00:00")))
                .andExpect(jsonPath("$.data.tripResponse.economyClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.confortClass", is(1073741823)))
                .andExpect(jsonPath("$.data.tripResponse.priceForEconomyClass", is("22.5")))
                .andExpect(jsonPath("$.data.tripResponse.priceForConfortClass", is("50.0")));
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
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("G1235", "Nan Jing", "Shang Hai");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
     * <li><b>Called by ts-travel-service:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body. However, startingPlace and / or endPlace are empty Strings
     * <li><b>Parameters:</b></li> a TripAllDetailInfo entity with a trip id that exists in the database, but the startingPlace and
     * endPlace are empty Strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetail with empty valyes. The service should not be
     * able to configure a proper TripAllDetail object, so the response should be similar to when the tripId does not exist in the database.
     * <li><b>Related Issue:</b></li> <b>F13b:</b> A request to the wrong endpoint is made. The service tries to call
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
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("D1345", "Shang Hai", "Su Zhou");
        tripAllDetailInfo.setFrom("");
        tripAllDetailInfo.setTo("");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
    @Order(6)
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

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo("G1234", "Nan Jing", "Shang Hai");

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
        tripId.setType(Type.G);
        tripId.setNumber(tripName);

        return tripRepository.findByTripId(tripId);
    }
}
