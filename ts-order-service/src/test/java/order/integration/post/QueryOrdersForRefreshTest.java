package order.integration.post;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import order.entity.Order;
import order.entity.OrderInfo;
import order.repository.OrderRepository;
import org.junit.jupiter.api.*;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryOrdersForRefreshTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.station.service.url", stationContainer::getHost);
        registry.add("ts.station.service.port", () -> stationContainer.getMappedPort(12345));
    }

    @BeforeEach
    public void setUp() {
        orderRepository.deleteAll();
    }

    /*
     * The equivalence based test is designed to verify that the endpoint for checking if the orders fit the requirements works correctly and updates the station IDs, for a valid order information
     * where the login ID matches an account ID of orders in the database.
     * It ensures that the endpoint returns a successful response with the appropriate message and correct orders.
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    public void testQueryOrdersForRefreshCorrectObject() throws Exception {
        // Arrange
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderRepository.save(order);
        ArrayList<Order> orderList = new ArrayList<>();
        orderList.add(order);
        order.setFrom("Nan Jing");
        order.setTo("Shang Hai Hong Qiao");

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        String result = mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(new Response<>(1, "Query Orders For Refresh Success", orderList), JSONObject.parseObject(result,  new TypeReference<Response<ArrayList<Order>>>(){}));
    }

    /**
     * <ul>
     * <li><b>Called by ts-order-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when no orders match the query criteria.
     * <li><b>Parameters:</b></li> a valid order information object with no orders in the database
     * <li><b>Expected result:</b></li> status 1, msg "Query Orders For Refresh Success", an empty list of orders
     * </ul>
     * @throws Exception
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    public void testQueryOrdersForRefreshNoOrdersFound() throws Exception {
        // Arrange
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Query Orders For Refresh Success")))
                .andExpect(jsonPath("$.data", is(new ArrayList<>())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-order-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> a valid order information object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>F20a:</b> when the station ids are invalid, the station service will return an empty list,
     * which is not checked by the order service. This leads to an IndexOutOfBoundsException in the order service and trigger a fallback response.<br>
     * <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    public void FAILING_testQueryOrdersForRefreshNoStationNameFound() throws Exception {
        // Arrange
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        //not existing station ids
        order.setFrom("Invalid");
        order.setTo("Invalid");
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderRepository.save(order);

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No station names found for the provided station IDs")))
                .andExpect(jsonPath("$.data", is(new ArrayList<>())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-order-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> a valid order information object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void testQueryOrdersForRefreshServiceUnavailable() throws Exception {
        // Arrange
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(false);
        info.setEnableBoughtDateQuery(false);
        info.setState(0);
        Order order = createSampleOrder();
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderRepository.save(order);

        // Stop the station service container to simulate service unavailability
        stationContainer.stop();

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Order createSampleOrder() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067702"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013"));
        order.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order.setContactsName("contactName");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("1");
        order.setFrom("nanjing");
        order.setTo("shanghaihongqiao");
        order.setStatus(0);
        order.setPrice("100.0");
        return order;
    }
}
