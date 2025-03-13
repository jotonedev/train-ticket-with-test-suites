package execute.component.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import execute.entity.Order;
import execute.entity.OrderStatus;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TicketExecuteTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Autowired
    private MockMvc mockMvc;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an existing ID as the URL parameter that can be found by
     * the ts-order-service.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId that can be found by the ts-order-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTicketExecuteCorrectIdOrderService() throws  Exception {
        // Arrange
        Order order = new Order();
        order.setStatus(2);
        order.setId(UUID.randomUUID());
        Response<Order> responseOrder = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        Response<Order> responseExecute = new Response<>(1, "Modify Order Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/status/" + order.getId() + "/" + OrderStatus.USED.getCode()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseExecute), MediaType.APPLICATION_JSON));

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", order.getId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more variables in the URL than it expects
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId that can be found by the ts-order-service. and some additional random UUIDs.
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTicketExecuteCorrectIdOrderServiceMultipleId() throws  Exception {
        // Arrange
        Order order = new Order();
        order.setStatus(2);
        order.setId(UUID.randomUUID());
        Response<Order> responseOrder = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        Response<Order> responseExecute = new Response<>(1, "Modify Order Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/status/" + order.getId() + "/" + OrderStatus.USED.getCode()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseExecute), MediaType.APPLICATION_JSON));

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", order.getId(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an orderID that can not be found by the ts-order-service or
     * the ts-order-other-service.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random Id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTicketExecuteIdNotExists() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();;
        Response<Order> responseOrder = new Response<>(0, "Order Not Found", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        Response<Order> responseOrderOther = new Response<>(0, "Order Not Found", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrderOther), MediaType.APPLICATION_JSON));

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", orderId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an orderID that cannot be found by the ts-order-service
     * but can be found by the ts-order-other-service.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId that can be found by the ts-order-other-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTicketExecuteCorrectIdOrderOtherService() throws Exception {
        // Arrange
        Order order = new Order();
        order.setStatus(OrderStatus.COLLECTED.getCode());
        order.setId(UUID.randomUUID());
        Response<Order> responseOrder = new Response<>(0, "Order Not Found", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + order.getId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        Response<Order> responseOrderOther = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + order.getId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrderOther), MediaType.APPLICATION_JSON));

        Response<Order> responseExecute = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/status/" + order.getId() + "/" + OrderStatus.USED.getCode()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseExecute), MediaType.APPLICATION_JSON));

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", order.getId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when nothing is passed to the endpoint
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> an IllegalArgumentException
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTicketExecuteMissingVariable() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testTicketExecuteMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }
}
