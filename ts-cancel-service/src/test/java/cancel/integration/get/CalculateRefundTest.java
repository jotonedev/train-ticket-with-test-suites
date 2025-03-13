package cancel.integration.get;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CalculateRefundTest {

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer insidePaymentServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-mongo");

    @Container
    public static MongoDBContainer userServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");

    @Container
    public static MongoDBContainer orderServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer orderOtherServiceMongoDbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");


    // requires docker image of ts-inside-payment-service with the name local/ts-inside-payment-service:0.1
    @Container
    public static GenericContainer<?> insidePaymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-inside-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18673)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service")
            .dependsOn(insidePaymentServiceMongoDbContainer);

    // requires docker image of ts-user-service with the name local/ts-user-payment-service:0.1
    @Container
    public static GenericContainer<?> userServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-user-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12342)
            .withNetwork(network)
            .withNetworkAliases("ts-user-service")
            .dependsOn(userServiceMongoDbContainer);

    // requires docker image of ts-order-service with the name local/ts-order-payment-service:0.1
    @Container
    public static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDbContainer);

    // requires docker image of ts-order-other-service with the name local/ts-order-other-payment-service:0.1
    @Container
    public static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDbContainer);

    @RegisterExtension
    static WireMockExtension notificationServiceWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(17853)).build();

    @BeforeAll
    static void setupWireMock() {
        configureFor("localhost", 17853);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.notification.service.url", () -> "localhost");
        registry.add("ts.notification.service.port",() -> "17853");
        registry.add("ts.inside.payment.service.url", insidePaymentServiceContainer::getHost);
        registry.add("ts.inside.payment.service.port", () -> insidePaymentServiceContainer.getMappedPort(18673));
        registry.add("ts.user.service.url", userServiceContainer::getHost);
        registry.add("ts.user.service.port", () -> userServiceContainer.getMappedPort(12342));
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order status is 'NOTPAID' as returned by the ts-order-service.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status NOTPAID
     * <li><b>Expected result:</b></li> status 1, msg "Success. Refoud 0", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testCalculateRefundNotPaidFromOrderService() throws Exception {
        String id = "5ad7750b-a68b-49c0-a8c0-32776b067703";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. Refoud 0")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order status is 'PAID' as returned by the ts-order-service.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status PAID
     * <li><b>Expected result:</b></li> status 1, msg "Success. ", "80,00"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testCalculateRefundPaidFromOrderService() throws Exception {
        String id = "5f50d821-5f22-44f6-b2de-5e79d4b29c68";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. ")))
                .andExpect(jsonPath("$.data", is("80,00")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order status is neither 'NOTPAID' nor 'PAID' (in this case 'CANCEL'),
     * and the order is found by the ts-order-service.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status CANCEL
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted, Refound error", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testCalculateRefundDifferentStatusFromOrderService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d7";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Status Cancel Not Permitted, Refound error")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order with status 'NOTPAID' is not found by the ts-order-service,
     * but by the ts-order-other-service.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-other-service with the order status NOTPAID
     * <li><b>Expected result:</b></li> status 1, msg "Success, Refound 0", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testCalculateRefundNotPaidFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d6";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success, Refound 0")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order with order status 'PAID' is not found by the order service,
     * but is found in the ts-order-other-service.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-other-service with the order status PAID
     * <li><b>Expected result:</b></li> status 1, msg "Success", "80,00"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testCalculateRefundPaidFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d5";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is("80,00")));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order service does not find the order with status neither
     * 'NOTPAID' nor 'PAID', but the order-other service does.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-other-service with the order status COLLECTED
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testCalculateRefundDifferentStatusFromOrderOtherService() throws Exception {
        String id = "f8e2dc60-bd59-4af9-bf15-507f3b6572d4";

        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Status Cancel Not Permitted")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order with the given orderId does not exist
     * <li><b>Parameters:</b></li> some random orderId that cannot be found by the order services
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(7)
    public void testCalculateRefundNonExistingId() throws Exception {
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-cancel-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
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
    @Order(8)
    public void testCalculateRefundUnavailableService() throws Exception {
        // Arrange
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        userServiceContainer.stop();
        insidePaymentServiceContainer.stop();

        String id = "5f50d821-5f22-44f6-b2de-5e79d4b29c68";

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
