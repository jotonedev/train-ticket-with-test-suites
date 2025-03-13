package fdse.microservice.integration.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import fdse.microservice.entity.Travel;
import fdse.microservice.entity.Trip;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryForTravelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer stationServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer routeServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static MongoDBContainer priceServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");

    // requires docker image of ts-route-service with the name local/ts-station-service:0.1
    @Container
    public static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDbContainer);

    // requires docker image of ts-train-service with the name local/ts-train-service:0.1
    @Container
    public static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDbContainer);

    // requires docker image of ts-route-service with the name local/ts-route-service:0.1
    @Container
    public static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDbContainer);

    // requires docker image of ts-price-service with the name local/ts-price-service:0.1
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
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when all services are available and return valid responses
     * <li><b>Parameters:</b></li> a valid request object chosen in a way that all services can respond with something from
     * their initdata file
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TravelResult
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testQueryForTravelValidObject() throws Exception {
        // Arrange
        Travel travel = generateTravel("Nan Jing", "Shang Hai", "a3f256c1-0e43-4f7d-9c21-121bf258101f", "GaoTieOne");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.status", is(true)))
                .andExpect(jsonPath("$.data.percent", is(1.0)))
                .andExpect(jsonPath("$.data.trainType.id", is("GaoTieOne")))
                .andExpect(jsonPath("$.data.prices.confortClass", is("250.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("175.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-station-service is unable to find the station
     * <li><b>Parameters:</b></li> a valid request object chosen in a way that all services can respond with something from
     * their initdata file except for the ts-station-service which can not find the station
     * <li><b>Expected result:</b></li> status 0, msg "Start place or end place not exist!", a configured TravelResult with a false status
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testQueryForTravelNotExistingPlaceNames() throws Exception {
        // Arrange
        Travel travel = generateTravel("Start not existing", "End not existing", "a3f256c1-0e43-4f7d-9c21-121bf258101f", "GaoTieOne");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Start place or end place not exist!")))
                .andExpect(jsonPath("$.data.status", is(false)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-train-service is unable to find the trainType
     * <li><b>Parameters:</b></li> a valid request object chosen in a way that all services can respond with something from
     * their initdata file except for the ts-train-service which can not find the trainType
     * <li><b>Expected result:</b></li> status 1, msg "Train type doesn't exist"
     * <li><b>Related Issue:</b></li> <b>F13a:</b> A request to an endpoint that does not exist is made (404). The called
     * train service fails to find a trainType, which will be forwarded to the price-service with an empty string as trainType.
     * @throws Exception
     */
    @Test
    @Order(3)
    public void FAILING_testQueryForTravelNotExistingTrainType() throws Exception {
        // Arrange
        Travel travel = generateTravel("Nan Jing", "Shang Hai", "a3f256c1-0e43-4f7d-9c21-121bf258101f", "NotAValidTrainType");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Train type doesn't exist")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-route-service is unable to find the route
     * <li><b>Parameters:</b></li> a valid request object chosen in a way that all services can respond with something from
     * their initdata file except for the ts-route-service which can not find the route
     * <li><b>Expected result:</b></li> status 1, msg Success, prices are the default error prices. Unlike in the component test,
     * the prices are not 0. This is because the prices and routes of the initData are linked, meaning that if there is no route,
     * there also is no price. Only when there is a price but no route can a price of 0 be returned.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testQueryForTravelNotExistingRoute() throws Exception {
        // Arrange
        Travel travel = generateTravel("Nan Jing", "Shang Hai", "invalid Id", "GaoTieOne");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.status", is(true)))
                .andExpect(jsonPath("$.data.prices.confortClass", is("120.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("95.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-price-service is unable to find the priceConfig
     * <li><b>Parameters:</b></li> a valid request object chosen in a way that all services can respond with something from
     * their initdata file except for the ts-price-service which can not find the priceConfig
     * <li><b>Expected result:</b></li> status 1, msg Success, prices are the default error prices
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testQueryForTravelNotExistingPriceConfig() throws Exception {
        // Arrange
        Travel travel = generateTravel("Nan Jing", "Shang Hai", "a3f256c1-0e43-4f7d-9c21-121bf258101f", "GaoTieTwo");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.status", is(true)))
                .andExpect(jsonPath("$.data.prices.confortClass", is("120.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("95.0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-basic-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
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
    public void testQueryForTravelUnavailableService() throws Exception {
        // Arrange
        stationServiceContainer.stop();
        trainServiceContainer.stop();
        routeServiceContainer.stop();
        priceServiceContainer.stop();

        Travel travel = generateTravel("Nan Jing", "Shang Hai", "a3f256c1-0e43-4f7d-9c21-121bf258101f", "GaoTieOne");

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travel)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Travel generateTravel(String startingPlace, String endPlace, String routeId, String trainTypeId) {
        Travel travel = new Travel();
        travel.setStartingPlace(startingPlace);
        travel.setEndPlace(endPlace);
        travel.setTrip(generateTrip(routeId, trainTypeId));

        return travel;
    }

    private Trip generateTrip(String routeId, String trainTypeId) {
        Trip trip = new Trip();
        trip.setRouteId(routeId);
        trip.setTrainTypeId(trainTypeId);
        return trip;
    }
}
