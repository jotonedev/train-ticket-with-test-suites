package rebook.integration.post;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
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

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PayDifferenceTest {

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");


    @Container
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");


    @Container
    public static MongoDBContainer insidePaymentServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-mongo");


    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer paymentServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-payment-mongo");

    @Container
    public static MongoDBContainer travelServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    public static MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");


    @Container
    private static GenericContainer<?> insidePaymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-inside-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18673)
            .withNetwork(network)
            .withNetworkAliases("ts-inside-payment-service")
            .dependsOn(insidePaymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> stationServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-station-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12345)
            .withNetwork(network)
            .withNetworkAliases("ts-station-service")
            .dependsOn(stationServiceMongoDBContainer);

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

    @Container
    private static GenericContainer<?> paymentServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-payment-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(19001)
            .withNetwork(network)
            .withNetworkAliases("ts-payment-service")
            .dependsOn(paymentServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    private static GenericContainer<?> travelServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel-service")
            .dependsOn(travelServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> ticketInfoServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-ticketinfo-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15681)
            .withNetwork(network)
            .withNetworkAliases("ts-ticketinfo-service");

    @Container
    private static GenericContainer<?> basicServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-basic-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15680)
            .withNetwork(network)
            .withNetworkAliases("ts-basic-service");

    @Container
    private static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);


    @BeforeAll
    public static void setUp() {
        stationServiceContainer.start();
        orderOtherServiceContainer.start();
        orderServiceContainer.start();
        insidePaymentServiceContainer.start();
        stationServiceContainer.start();
        paymentServiceContainer.start();
        travel2ServiceContainer.start();
        travelServiceContainer.start();
        priceServiceContainer.start();
        trainServiceContainer.start();
        routeServiceContainer.start();
        seatServiceContainer.start();
        ticketInfoServiceContainer.start();
        basicServiceContainer.start();
        configServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.inside.payment.service.url", insidePaymentServiceContainer::getHost);
        registry.add("ts.inside.payment.service.port", () -> insidePaymentServiceContainer.getMappedPort(18673));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.payment.service.url", paymentServiceContainer::getHost);
        registry.add("ts.payment.service.port", () -> paymentServiceContainer.getMappedPort(19001));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.travel.service.url", travelServiceContainer::getHost);
        registry.add("ts.travel.service.port", () -> travelServiceContainer.getMappedPort(12346));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.ticketinfo.service.url", ticketInfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketInfoServiceContainer.getMappedPort(15681));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));

    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request object is correct
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object
     * <li><b>Related Issue:</b></li> <b>F24:</b> The deleteOrder method inside the service implementation is supposed
     * to call the DELETE endpoint on the order services. However, this endpoint is mistakenly called with a POST request, which
     * does not exist and therefore causes an error.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    void FAILING_testPayDifferenceCorrectObject() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.documentType", is(1)))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(3)))
                .andExpect(jsonPath("$.data.price", is("250.0")))
                .andExpect(jsonPath("$.data.from", is("nanjing")))
                .andExpect(jsonPath("$.data.contactsName", is("Contacts_One")))
                .andExpect(jsonPath("$.data.contactsDocumentNumber", is("DocumentNumber_One")))
                .andExpect(jsonPath("$.data.id", is("5ac7750c-a68c-49c0-a8c0-32776c067703")))
                .andExpect(jsonPath("$.data.accountId", is("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")))
                .andExpect(jsonPath("$.data.trainNumber", is("G1234")))
                .andExpect(jsonPath("$.data.to", is("shanghai")))
                .andExpect(jsonPath("$.data.status", is(3)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the payment of difference fails (inside-payment service returns status 0)
     * <li><b>Parameters:</b></li> a valid request object, but there is already a payment object with the orderId in the database
     * <li><b>Expected result:</b></li> status 0, msg "Can't pay the difference,please try again", no data
     * <li><b>Related Issue:</b></li> <b>F14:</b> The inside-payment service uses the library for BigDecimal incorrectly, which
     * causes the total expense to always be 0. This means that the paymentService is never called, causing the wrong response.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    void FAILING_testPayDifferencePaymentFailed() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                        " \"orderId\":\"5ad7750b-a68b-49c0-a8c0-32776b067701\"," +
                        " \"oldTripId\":\"G9999\"," +
                        " \"tripId\":\"G1234\"," +
                        " \"seatType\":3," +
                        " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Can't pay the difference,please try again")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> <li><b>Tests:</b></li> how the endpoint behaves when the loginId does not exist
     * <li><b>Parameters:</b></li> a valid request object with a non-existing loginId
     * <li><b>Expected result:</b></li> status 1, msg "Success!", a configured order object. payment object will be
     * created with this new loginId in the paymentService
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    void testPayDifferenceLoginIdNonExisting() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-0000-0000-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"G1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success!")))
                .andExpect(jsonPath("$.data.documentType", is(1)))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(2)))
                .andExpect(jsonPath("$.data.price", is("250.0")))
                .andExpect(jsonPath("$.data.from", is("nanjing")))
                .andExpect(jsonPath("$.data.contactsName", is("Contacts_One")))
                .andExpect(jsonPath("$.data.contactsDocumentNumber", is("DocumentNumber_One")))
                .andExpect(jsonPath("$.data.id", is("5ac7750c-a68c-49c0-a8c0-32776c067703")))
                .andExpect(jsonPath("$.data.accountId", is("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")))
                .andExpect(jsonPath("$.data.trainNumber", is("G1234")))
                .andExpect(jsonPath("$.data.to", is("shanghai")))
                .andExpect(jsonPath("$.data.status", is(3)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the orderId does not exist
     * <li><b>Parameters:</b></li> a valid request object with a non-existing orderId
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    void testPayDifferenceOrderIdNonExisting() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-0000-0000-32776c067703\"," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId does not exist
     * <li><b>Parameters:</b></li> a valid request object with a non-existing tripId
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    void testPayDifferenceTripIdNonExisting() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when both tripIds are from the same type, which means they both begin with
     * "G" or "D" or both do not.
     * <li><b>Parameters:</b></li> a valid request object where both tripIds are from the same type
     * <li><b>Expected result:</b></li> status 1, msg "Success!", a configured order. The order is not deleted and newly created
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    void testPayDifferenceTripIdSameType() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"G9999\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success!")))
                .andExpect(jsonPath("$.data.documentType", is(1)))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(2)))
                .andExpect(jsonPath("$.data.price", is("250.0")))
                .andExpect(jsonPath("$.data.from", is("nanjing")))
                .andExpect(jsonPath("$.data.contactsName", is("Contacts_One")))
                .andExpect(jsonPath("$.data.contactsDocumentNumber", is("DocumentNumber_One")))
                .andExpect(jsonPath("$.data.id", is("5ac7750c-a68c-49c0-a8c0-32776c067703")))
                .andExpect(jsonPath("$.data.accountId", is("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")))
                .andExpect(jsonPath("$.data.trainNumber", is("G1234")))
                .andExpect(jsonPath("$.data.to", is("shanghai")))
                .andExpect(jsonPath("$.data.status", is(3)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is neither 2 nor 3
     * <li><b>Parameters:</b></li> a valid request object with seatType 0
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(7)
    void testPayDifferenceSeatTypeInvalid() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067705\"," +
                " \"oldTripId\":\"G9999\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":0," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success!")))
                .andExpect(jsonPath("$.data.documentType", is(1)))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(3)))
                .andExpect(jsonPath("$.data.price", is("0")))
                .andExpect(jsonPath("$.data.from", is("nanjing")))
                .andExpect(jsonPath("$.data.contactsName", is("Contacts_One")))
                .andExpect(jsonPath("$.data.contactsDocumentNumber", is("DocumentNumber_One")))
                .andExpect(jsonPath("$.data.id", is("5ac7750c-a68c-49c0-a8c0-32776c067705")))
                .andExpect(jsonPath("$.data.accountId", is("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")))
                .andExpect(jsonPath("$.data.trainNumber", is("G1234")))
                .andExpect(jsonPath("$.data.to", is("shanghai")))
                .andExpect(jsonPath("$.data.status", is(3)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> if the old ticket price is higher than the new one
     * <li><b>Parameters:</b></li> a valid request object with old TripId "G9999" and new TripId "G1234", making the old ticket
     * price higher than the new one
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(8)
    void testPayDifferenceNegativeDifference() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067705\"," +
                " \"oldTripId\":\"G9999\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":3," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success!")))
                .andExpect(jsonPath("$.data.documentType", is(1)))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(3)))
                .andExpect(jsonPath("$.data.price", is("95.0")))
                .andExpect(jsonPath("$.data.from", is("nanjing")))
                .andExpect(jsonPath("$.data.contactsName", is("Contacts_One")))
                .andExpect(jsonPath("$.data.contactsDocumentNumber", is("DocumentNumber_One")))
                .andExpect(jsonPath("$.data.id", is("5ac7750c-a68c-49c0-a8c0-32776c067705")))
                .andExpect(jsonPath("$.data.accountId", is("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f")))
                .andExpect(jsonPath("$.data.trainNumber", is("G1234")))
                .andExpect(jsonPath("$.data.to", is("shanghai")))
                .andExpect(jsonPath("$.data.status", is(3)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the loginId has the wrong format
     * <li><b>Parameters:</b></li> a valid request object with a loginId that is null
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(9)
    void testPayDifferenceLoginIdWrongFormat() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":null," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":3," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the loginId has the wrong format
     * <li><b>Parameters:</b></li> a valid request object with a loginId that is null
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(10)
    void testPayDifferenceOrderIdWrongFormat() throws Exception {
        // Arrange
        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":null," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":3," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
    
    /**
     * <ul>
     * <li><b>Called by ts-rebook-service:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-order-other-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-payment-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-config-service</li>
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
    @Order(11)
    void testPayDifferenceUnavailableService() throws Exception {
        // Arrange
        insidePaymentServiceContainer.stop();
        stationServiceContainer.stop();
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();
        paymentServiceContainer.stop();
        seatServiceContainer.stop();
        travelServiceContainer.stop();
        travel2ServiceContainer.stop();
        routeServiceContainer.stop();
        trainServiceContainer.stop();
        configServiceContainer.stop();
        ticketInfoServiceContainer.stop();
        basicServiceContainer.stop();
        priceServiceContainer.stop();

        String requestJson =
                "{\"loginId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\"," +
                " \"orderId\":\"5ac7750c-a68c-49c0-a8c0-32776c067703\"," +
                " \"oldTripId\":\"Z1237\"," +
                " \"tripId\":\"G1234\"," +
                " \"seatType\":2," +
                " \"date\":\"2026-01-01\"}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook/difference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
