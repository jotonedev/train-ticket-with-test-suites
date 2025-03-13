package inside_payment.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import inside_payment.entity.*;
import inside_payment.repository.AddMoneyRepository;
import inside_payment.repository.PaymentRepository;
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
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PayTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Autowired
    protected AddMoneyRepository addMoneyRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

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
    void beforeEach() {
        paymentRepository.deleteAll();
        addMoneyRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that a new payment can be created when the user has enough balance.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a PaymentInfo object with a price the user can afford
     * <li><b>Expected result:</b></li> status 1, msg "Payment Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayCorrectObject() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("123");
        paymentInfo.setOrderId(UUID.randomUUID().toString());
        paymentInfo.setPrice("50"); // The user has enough balance to pay this price (200)
        paymentInfo.setTripId("G");

        Order order = new Order();
        order.setId(UUID.fromString(paymentInfo.getOrderId()));
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setPrice("50.0");

        Response<Order> responseOrder = new Response<>(1, "Success.", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/" + paymentInfo.getOrderId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        order.setStatus(OrderStatus.PAID.getCode());
        Response<Order> responseOrderStatus = new Response<>(1, "Modify Order Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/status/" + order.getId() + "/" + OrderStatus.PAID.getCode()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrderStatus), MediaType.APPLICATION_JSON));

        addMoneyRepository.save(createSampleMoney());
        paymentRepository.save(createSamplePayment());
        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Payment Success")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li>that a new payment can be created when the user has not enough balance.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a PaymentInfo object with a price that the user cannot afford
     * <li><b>Expected result:</b></li> status 1, msg "Payment Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayNotEnoughBalance() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("123");
        paymentInfo.setOrderId(UUID.randomUUID().toString());
        paymentInfo.setPrice("300.0"); // The user has only 200.0
        paymentInfo.setTripId("A");

        Order order = new Order();
        order.setId(UUID.fromString(paymentInfo.getOrderId()));
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setPrice("300.0");

        Response<Order> responseOrder = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + paymentInfo.getOrderId()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrder), MediaType.APPLICATION_JSON));

        addMoneyRepository.save(createSampleMoney());
        paymentRepository.save(createSamplePayment());

        Response<String> responsePayment = new Response<>(1, "Pay Success", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-payment-service:19001/api/v1/paymentservice/payment").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responsePayment), MediaType.APPLICATION_JSON));

        order.setStatus(OrderStatus.PAID.getCode());
        Response<Order> responseOrderStatus = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/status/" + order.getId() + "/" + OrderStatus.PAID.getCode()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseOrderStatus), MediaType.APPLICATION_JSON));

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Payment Success " + responsePayment.getMsg())))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two PaymentInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayMultipleObjects() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        PaymentInfo[] infos = {paymentInfo, paymentInfo};
        String jsonRequest = new ObjectMapper().writeValueAsString(infos);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
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
    public void testPayMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
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
    public void testPayEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
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
    public void testPayNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private Money createSampleMoney() {
        Money money = new Money();
        money.setUserId("123");
        money.setMoney("200.0");
        return money;
    }

    private Payment createSamplePayment() {
        Payment payment = new Payment();
        payment.setOrderId(UUID.randomUUID().toString());
        payment.setPrice("100.0");
        payment.setUserId("123");
        payment.setType(PaymentType.P);
        return payment;
    }
}
