package plan.integration.post;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import plan.entity.RoutePlanInfo;

import java.util.Date;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchQuickestResultTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.travel.service.url:ts-travel-service}")
    private String tsTravelServiceUrl;

    @Value("${ts.travel.service.port:12346}")
    private String tsTravelServicePort;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    private static GenericContainer<?> travelContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static GenericContainer<?> travel2Container = new GenericContainer<>(DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static GenericContainer<?> stationContainer = new GenericContainer<>(DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static GenericContainer<?> routeContainer = new GenericContainer<>(DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    public static GenericContainer<?> ticketInfoContainer = new GenericContainer<>(DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");

    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static GenericContainer<?> orderContainer = new GenericContainer<>(DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    public static GenericContainer<?> orderOtherContainer = new GenericContainer<>(DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    public static GenericContainer<?> seatContainer = new GenericContainer<>(DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    public static final MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static GenericContainer<?> trainContainer = new GenericContainer<>(DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    public static GenericContainer<?> basicContainer = new GenericContainer<>(DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @Container
    public static final MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");

    @Container
    public static GenericContainer<?> priceContainer = new GenericContainer<>(DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    public static GenericContainer<?> configContainer = new GenericContainer<>(DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.travel.service.url", travelContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelContainer.getMappedPort(12346));
        registry.add("ts.travel2.service.url", travel2Container::getHost);
        registry.add("ts.travel2.service.port", () -> travel2Container.getMappedPort(16346));
        registry.add("ts.station.service.url", stationContainer::getHost);
        registry.add("ts.station.service.port", () -> stationContainer.getMappedPort(12345));
        registry.add("ts.route.service.url", routeContainer::getHost);
        registry.add("ts.route.service.port", () -> routeContainer.getMappedPort(11178));
        registry.add("ts.ticketinfo.service.url", ticketInfoContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketInfoContainer.getMappedPort(15681));
        registry.add("ts.order.service.url", orderContainer::getHost);
        registry.add("ts.order.service.port", () -> orderContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherContainer.getMappedPort(12032));
        registry.add("ts.seat.service.url", seatContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatContainer.getMappedPort(18898));
        registry.add("ts.train.service.url", trainContainer::getHost);
        registry.add("ts.train.service.port", () -> trainContainer.getMappedPort(14567));
        registry.add("ts.basic.service.url", basicContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicContainer.getMappedPort(15680));
        registry.add("ts.price.service.url", priceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceContainer.getMappedPort(16579));
        registry.add("ts.config.service.url", configContainer::getHost);
        registry.add("ts.config.service.port", () -> configContainer.getMappedPort(15679));
    }

    /**
     * <ul>
     * <li><b>Called by ts-route-plan-service:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-seat-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are no trips found with the given station names
     * <li><b>Parameters:</b></li> a RoutePlanInfo chosen in a way such that there are no trips found with the given station names
     * <li><b>Expected result:</b></li> status 1, msg "Success", an empty list of RoutePlanResultUnits
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testSearchQuickestResultNoTrips() throws Exception {
        // Arrange
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Station A", "Station B", new Date("Mon May 04 09:00:00 GMT+0800 2025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-route-plan-service:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-seat-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when trips are found through valid and existing station names
     * <li><b>Parameters:</b></li> a fully configured RoutePlanInfo object with existing station names
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured list of RoutePlanResultUnits
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testSearchQuickestResultCorrectObject() throws Exception {
        // Arrange
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Nan Jing", "Bei Jing", new Date("Sat Jul 29 00:00:00 GMT+0800 3025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId", is("Z1235")))
                .andExpect(jsonPath("$.data[0].trainTypeId", is("ZhiDa")))
                .andExpect(jsonPath("$.data[0].fromStationName", is("Nan Jing")))
                .andExpect(jsonPath("$.data[0].toStationName", is("Bei Jing")));
    }


    /**
     * <ul>
     * <li><b>Called by ts-route-plan-service:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-seat-service</li>
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
    @Order(3)
    public void testSearchQuickestResultServiceUnavailable() throws Exception {
        // Arrange
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        travelContainer.stop();
        travel2Container.stop();
        stationContainer.stop();
        ticketInfoContainer.stop();
        basicContainer.stop();
        orderContainer.stop();
        orderOtherContainer.stop();
        routeContainer.stop();
        trainContainer.stop();
        priceContainer.stop();
        configContainer.stop();
        seatContainer.stop();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
