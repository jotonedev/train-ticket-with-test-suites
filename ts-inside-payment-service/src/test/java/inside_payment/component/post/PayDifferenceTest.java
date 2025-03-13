package inside_payment.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PayDifferenceTest {

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
     * <li><b>Parameters:</b></li> a PaymentInfo object with a price the user can afford
     * <li><b>Expected result:</b></li> status 1, msg "Pay Difference Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayDifferenceCorrectObject() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("123");
        paymentInfo.setOrderId("1");
        paymentInfo.setPrice("50"); // The user has enough balance to pay this price (200)
        addMoneyRepository.save(createSampleMoney());
        paymentRepository.save(createSamplePayment());
        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Difference Success")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that a new payment can be created when the user has not enough balance.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a PaymentInfo object with a price that the user cannot afford
     * <li><b>Expected result:</b></li> status 1, msg "Pay Difference Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayDifferenceNotEnoughBalance() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("123");
        paymentInfo.setOrderId("1");
        paymentInfo.setPrice("300.0"); // The user has only 200.0
        addMoneyRepository.save(createSampleMoney());
        paymentRepository.save(createSamplePayment());

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Difference Success")))
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
    public void testPayDifferenceMultipleObjects() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        PaymentInfo[] infos = {paymentInfo, paymentInfo};
        String jsonRequest = new ObjectMapper().writeValueAsString(infos);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
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
    public void testPayDifferenceMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
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
    public void testPayDifferenceEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
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
    public void testPayDifferenceNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment/difference")
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
