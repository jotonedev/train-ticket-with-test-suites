package adminorder.integration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import edu.fudan.common.util.Response;
import foodsearch.FoodApplication;
import foodsearch.entity.AllTripFood;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = { FoodApplication.class })
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetAllFoodTest {

    private final static Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Container
    public static final MongoDBContainer foodMapServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-food-map-mongo");

    @Container
    public static GenericContainer<?> foodMapContainer = new GenericContainer<>(DockerImageName.parse("local/ts-food-map-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18855)
            .withNetwork(network)
            .withNetworkAliases("ts-food-map-service")
            .dependsOn(foodMapServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
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
    public static final MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static GenericContainer<?> travelContainer = new GenericContainer<>(DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    public static final MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static GenericContainer<?> routeContainer = new GenericContainer<>(DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);



    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.food.map.service.url", foodMapContainer::getHost);
        registry.add("ts.food.map.service.port", () -> foodMapContainer.getMappedPort(18855));
        registry.add("ts.station.service.url", stationContainer::getHost);
        registry.add("ts.station.service.port", () -> stationContainer.getMappedPort(12345));
        registry.add("ts.travel.service.url", travelContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelContainer.getMappedPort(12346));
        registry.add("ts.route.service.url", routeContainer::getHost);
        registry.add("ts.route.service.port", () -> routeContainer.getMappedPort(11178));
    }

    /**
     * <ul>
     * <li><b>Called by ts-food-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-food-map-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving all trip food works correctly, for all valid path
     * variables that have matching objects in the database
     * <li><b>Parameters:</b></li> a valid request object with data that can be utilized by the external services.
     * <li><b>Expected result:</b></li> status 1, msg "Get All Food Success", A configured AllTripFood object.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testGetAllFoodCorrectObject() throws Exception {
        // Arrange
        String startStation = "Nan Jing";
        String endStation = "Shang Hai";
        String tripId = "G1234";

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get All Food Success")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AllTripFood object
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(1, response.getData().getTrainFoodList().size());
        Assertions.assertEquals("G1234", response.getData().getTrainFoodList().get(0).getTripId());
        Assertions.assertEquals(2, response.getData().getFoodStoreListMap().get("shanghai").size());
        Assertions.assertEquals(3, response.getData().getFoodStoreListMap().get("nanjing").size());
    }

    /**
     * <ul>
     * <li><b>Called by ts-food-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-food-map-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving all trip food works correctly, if there are no station ids found.
     * <li><b>Parameters:</b></li> a valid request object with random station names that cannot be found by the ts-station-service.
     * <li><b>Expected result:</b></li> status 0, msg "Get All Food Failed", an empty AllTripFood object.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetAllFoodStationsNotFound() throws Exception {
        // Arrange
        String startStation = UUID.randomUUID().toString();
        String endStation = UUID.randomUUID().toString();
        String tripId = "G1234";

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AllTripFood object
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(new AllTripFood(), response.getData());
    }

    /**
     * <ul>
     * <li><b>Called by ts-food-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-food-map-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that the endpoint for retrieving all trip food works correctly, if there is no route found.
     * <li><b>Parameters:</b></li> a valid request object with a tripId that cannot be found by the ts-route-service.
     * <li><b>Expected result:</b></li> status 0, msg "Get All Food Failed", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testGetAllNoRouteFound() throws Exception {
        // Arrange
        String startStation = "Nan Jing";
        String endStation = "Shang Hai";
        String tripId = "K1245";

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Get All Food Failed")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AllTripFood object
        Response<AllTripFood> response = JSONObject.parseObject(result, new TypeReference<Response<AllTripFood>>() {});
        Assertions.assertEquals(new AllTripFood(), response.getData());
    }

    /**
     * <ul>
     * <li><b>Called by ts-food-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-food-map-service</li>
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
    @Order(4)
    public void testGetAllFoodServiceUnavailable() throws Exception {
        // Arrange
        String startStation = "Nan Jing";
        String endStation = "Shang Hai";
        String tripId = "G1234";

        foodMapContainer.stop();
        travelContainer.stop();
        routeContainer.stop();
        stationContainer.stop();

        // Act
        mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
