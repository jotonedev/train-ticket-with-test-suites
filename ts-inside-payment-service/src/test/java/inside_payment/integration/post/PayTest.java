package inside_payment.integration.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import inside_payment.entity.PaymentInfo;
import inside_payment.entity.PaymentType;
import inside_payment.repository.AddMoneyRepository;
import inside_payment.repository.PaymentRepository;
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
import org.testcontainers.utility.DockerImageName;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PayTest {

    private final static Network network = Network.newNetwork();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddMoneyRepository addMoneyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));


    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
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
    public static final MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
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
    public static final MongoDBContainer paymentServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-payment-mongo");

    @Container
    public static GenericContainer<?> paymentContainer = new GenericContainer<>(DockerImageName.parse("local/ts-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(19001)
            .withNetwork(network)
            .withNetworkAliases("ts-payment-service")
            .dependsOn(paymentServiceMongoDBContainer);


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.order.service.url", orderContainer::getHost);
        registry.add("ts.order.service.port", () -> orderContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherContainer.getMappedPort(12032));
        registry.add("ts.payment.service.url", paymentContainer::getHost);
        registry.add("ts.payment.service.port", () -> paymentContainer.getMappedPort(19001));
    }

    @BeforeEach
    public void setUp() {
        addMoneyRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Called by ts-inside-payment-service:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that a new payment can be created successfully when the order-service is called
     * <li><b>Parameters:</b></li> a fully configured PaymentInfo object with a tripId that will call the ts-order-service
     * <li><b>Expected result:</b></li> status 1, msg "Payment Success Pay Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testPayCorrectObjectOrderService() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Payment Success Pay Success")))
                .andExpect(jsonPath("$.data", nullValue()));

        Assertions.assertEquals(PaymentType.O, paymentRepository.findByOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703").get(0).getType());
    }

    /**
     * <ul>
     * <li><b>Called by ts-inside-payment-service:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that a new payment can be created successfully when the order-other-service is called
     * <li><b>Parameters:</b></li> a fully configured PaymentInfo object with a tripId that will call the ts-order-other-service
     * <li><b>Expected result:</b></li> status 1, msg "Payment Success Pay Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testPayCorrectObjectOrderOtherService() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("4d2a46c7-70cb-4cf1-c5bb-b68406d9da6e");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("K1235");

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Payment Success Pay Success")))
                .andExpect(jsonPath("$.data", nullValue()));

        Assertions.assertEquals(PaymentType.O, paymentRepository.findByOrderId(paymentInfo.getOrderId()).get(0).getType());
    }

    /**
     * <ul>
     * <li><b>Called by ts-inside-payment-service:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order does not exist
     * <li><b>Parameters:</b></li> a PaymentInfo object with a non-existent orderId
     * <li><b>Expected result:</b></li> status 0, msg "Payment Failed, Order Not Exists", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testPayOrderNotFound() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad7750b-a64b-49c0-a8c0-32776b067700");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Payment Failed, Order Not Exists")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-inside-payment-service:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the price is negative
     * <li><b>Parameters:</b></li> a PaymentInfo object with a negative price
     * <li><b>Expected result:</b></li> status 0, msg "Payment Failed", no data
     * <li><b>Related Issue:</b></li> <b>D4:</b> For this specific endpoint, userID, and especially the price is irrelevant
     * for the functionality of this endpoint. The inside-payment service calls the order service with the orderID from the
     * request body, and the found order itself contains the price, which will be used instead of the provided price of the
     * paymentInfo, which is reasonable in its logic. The fact that you could however provide values which won't be utilized
     * by the service is a bad design choice.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void FAILING_testPayNegativePrice() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("4d2a46c7-70cb-4ce1-c5bb-b68406d9da6e");
        paymentInfo.setPrice("-50.");
        paymentInfo.setTripId("K1235");

        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Payment Failed")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-inside-payment-service:</b></li>
     *   <ul>
     *   <li>ts-payment-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> a fully configured PaymentInfo object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testPayUnavailableService() throws Exception {
        // Arrange
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setUserId("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        paymentInfo.setOrderId("5ad6650b-a68b-49c0-a8c0-32776b067703");
        paymentInfo.setPrice("50.");
        paymentInfo.setTripId("G1237");
        String jsonRequest = new ObjectMapper().writeValueAsString(paymentInfo);

        paymentContainer.stop();
        orderContainer.stop();
        orderOtherContainer.stop();

        // Act
        mockMvc.perform(post("/api/v1/inside_pay_service/inside_payment")
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
}
