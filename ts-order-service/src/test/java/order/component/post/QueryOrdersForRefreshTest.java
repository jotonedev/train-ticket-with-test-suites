package order.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import order.entity.Order;
import order.entity.OrderInfo;
import order.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryOrdersForRefreshTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
    }

    @BeforeEach
    void beforeEach() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        orderRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> the endpoint for checking if the orders fit the requirements works correctly and updates the station IDs, for a valid order information
     * where the login ID matches an account ID of orders in the database.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid OrderInfo object
     * <li><b>Expected result:</b></li> status 1, msg "Query Orders For Refresh Success", a list of orders
     * </ul>
     * @throws Exception
     */
    @Test
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
        order.setFrom("123");
        order.setTo("321");

        List<String> stationList = new ArrayList<>();
        stationList.add("123");
        stationList.add("321");
        Response<List<String>> responseTrainFood = new Response<>(1, "Success", stationList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/namelist").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        CollectionType collectionType = new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, Order.class);

        // Act
        String result = mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<ArrayList<Order>> response = new ObjectMapper().readValue(result, new ObjectMapper().getTypeFactory().constructParametricType(Response.class, collectionType));
        Assertions.assertEquals(new Response<>(1, "Query Orders For Refresh Success", orderList), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travelDateEnd is after the orders travelDate and the
     * travelDateStart is before the orders travelDate.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid orderInfo object with a travelDateEnd after the orders travelDate and a travelDateStart before the orders travelDate
     * <li><b>Expected result:</b></li> status 1, msg "Query Orders For Refresh Success", a list of orders. TravelDatePassFlag is true.
     * <li><b>Related Issue:</b></li> <b>F18a:</b> travelDate of the order is not correctly compered to travelDateStart of the orderInfo object,
     * but with the boughtDateStart of the orderInfo object, which does not make sense logically. Therefore, this test is failing,
     * as the boughtDateStart is not defined (and should not need to be)
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testQueryOrdersForRefreshCorrectTravelDatePassFlag() throws Exception {
        // Arrange
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());
        info.setEnableStateQuery(true);
        info.setEnableTravelDateQuery(true);
        info.setEnableBoughtDateQuery(false);
        info.setTravelDateEnd(new Date("Sat Jul 29 00:00:00 GMT+0800 2018")); // after the travel date
        info.setTravelDateStart(new Date("Sat Jul 29 00:00:00 GMT+0800 2016")); // before the travel date
        info.setState(0);

        Order order = createSampleOrder();
        order.setAccountId(UUID.fromString(info.getLoginId()));
        orderRepository.save(order);

        ArrayList<Order> orderList = new ArrayList<>();
        orderList.add(order);
        order.setFrom("123");
        order.setTo("321");

        List<String> stationList = new ArrayList<>();
        stationList.add("123");
        stationList.add("321");
        Response<List<String>> responseTrainFood = new Response<>(1, "Success", stationList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/namelist").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        CollectionType collectionType = new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, Order.class);

        // Act
        String result = mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<ArrayList<Order>> response = new ObjectMapper().readValue(result, new ObjectMapper().getTypeFactory().constructParametricType(Response.class, collectionType));
        Assertions.assertEquals(new Response<>(1, "Query Orders For Refresh Success", orderList), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Order objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryOrdersForRefreshMultipleObjects() throws Exception {
        // Arrange
        OrderInfo[] orders = {new OrderInfo(), new OrderInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryOrdersForRefreshMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryOrdersForRefreshEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryOrdersForRefreshNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private Order createSampleOrder() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067703"));
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
