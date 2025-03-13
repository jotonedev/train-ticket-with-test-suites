package cancel.integration.get;

import com.alibaba.fastjson.JSONObject;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.fudan.common.util.Response;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CancelOrderTest {

    private final String loginId = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer insidePaymentServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-mongo");

    @Container
    public static MongoDBContainer userServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            "mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");


    @Container
    private static GenericContainer<?> insidePaymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-inside-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18673)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service")
            .dependsOn(insidePaymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> userServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-user-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12342)
            .withNetwork(network)
            .withNetworkAliases("ts-user-service")
            .dependsOn(userServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @RegisterExtension
    static WireMockExtension notificationServiceWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(17853)).build();

    @BeforeAll
    static void setUpWireMock() {
        configureFor("localhost", 17853);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.notification.service.url", () -> "localhost");
        registry.add("ts.notification.service.port", () -> "17853");
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that an order with status 'NOTPAID' can be successfully canceled through the ts-order-service,
     * even if the payment refund fails.
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status NOTPAID
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testCancelOrderNotPaidSuccessFromOrderService() throws Exception {
        String orderId = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992b";
        stubNotificationService();

        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));

        verifyStubNotificationService();
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' cannot be canceled through the ts-order-service
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status COLLECTED
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testCancelOrderDifferentStatusFromOrderService() throws Exception {
        String orderId = "f8e2dc60-bd59-4af9-bf15-507f3b6572d7";

        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertEquals(response.getData(), null);
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that an order with status neither 'PAID', 'NOTPAID' or 'CHANGE' cannot be canceled through the ts-order-other-service
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-other-service with the order status COLLECTED
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testCancelOrderDifferentStatusFromOrderOtherService() throws Exception {
        String orderId = "f8e2dc60-bd59-4af9-bf15-507f3b6572d4";

        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Response<Object> response = JSONObject.parseObject(result, Response.class);
        Assertions.assertEquals(response.getStatus(), 0);
        Assertions.assertEquals(response.getMsg(), "Order Status Cancel Not Permitted");
        Assertions.assertNull(response.getData());
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> that an order with status 'NOTPAID' can be successfully canceled through the ts-order-other-service,
     * and the refund successfully drawn back
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-other-service with the order status NOTPAID
     * <li><b>Expected result:</b></li> status 0, msg "Cann't find userinfo by user id.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testCancelOrderNotPaidSuccessFromOrderOtherService() throws Exception {
        String orderId = "f8e2dc60-bd59-4af9-bf15-507f3b6572d6";

        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(1, "Success.", null), JSONObject.parseObject(result, Response.class));
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the service behaves when the accountId in the order does not match with a user saved in ts-user-service
     * <li><b>Parameters:</b></li> an orderId that belongs to an order in the repository in ts-order-service with the order status COLLECTED
     * <li><b>Expected result:</b></li> status 0, msg "Cann't find userinfo by user id.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testCancelOrderNotPaidWrongUserInfoFromOrderService() throws Exception {
        String orderId = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992c";
        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Cann't find userinfo by user id.", null), JSONObject.parseObject(result, Response.class));
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the service behaves when the order ID does not exist in the order service or the order-other service
     * <li><b>Parameters:</b></li> a random orderId that does not exist in the order-service or the order-other service
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testCancelOrderNonExistingId() throws Exception {
        String result = mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", UUID.randomUUID().toString(), loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(new Response<>(0, "Order Not Found.", null), JSONObject.parseObject(result, Response.class));
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
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available (including the notification service,
     * which will not be mocked in this case)
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> no content, as the controller catches the exception and returns null
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(7)
    public void testCancelOrderUnavailableService() throws Exception {
        String orderId = "e9cb042e-54d3-4b56-b5fa-67ff1c1c992b";

        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        insidePaymentServiceContainer.stop();
        userServiceContainer.stop();

        mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    private void stubNotificationService() {
        stubFor(post(urlEqualTo("/api/v1/notifyservice/notification/order_cancel_success"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));
    }

    private void verifyStubNotificationService() {
        verify(postRequestedFor(urlEqualTo("/api/v1/notifyservice/notification/order_cancel_success"))
                .withRequestBody(containing("fdse_microservice")));
    }
}
