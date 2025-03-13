package preserveOther.integration.post;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PreserveTest {
    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static MongoDBContainer stationServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-station-mongo");

    @Container
    public static MongoDBContainer travel2ServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-mongo");

    @Container
    public static MongoDBContainer assuranceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-assurance-mongo");


    @Container
    public static MongoDBContainer consignServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-consign-mongo");

    @Container
    public static MongoDBContainer foodServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-food-mongo");

    @Container
    public static MongoDBContainer contactsServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-contacts-mongo");

    @Container
    public static MongoDBContainer securityServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-security-mongo");


    @Container
    public static MongoDBContainer userServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-user-mongo");


    @Container
    private static GenericContainer<?> consignServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-consign-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16111)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-service")
            .dependsOn(consignServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> foodServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-food-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18856)
            .withNetwork(network)
            .withNetworkAliases("ts-food-service")
            .dependsOn(foodServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> contactsServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-contacts-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12347)
            .withNetwork(network)
            .withNetworkAliases("ts-contacts-service")
            .dependsOn(contactsServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> securityServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-security-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11188)
            .withNetwork(network)
            .withNetworkAliases("ts-security-service")
            .dependsOn(securityServiceMongoDBContainer);

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
    public static MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    private static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> travel2ServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-travel2-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16346)
            .withNetwork(network)
            .withNetworkAliases("ts-travel2-service")
            .dependsOn(travel2ServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> seatServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-seat-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18898)
            .withNetwork(network)
            .withNetworkAliases("ts-seat-service");

    @Container
    private static GenericContainer<?> assuranceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-assurance-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(18888)
            .withNetwork(network)
            .withNetworkAliases("ts-assurance-service")
            .dependsOn(assuranceServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> userServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-user-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12342)
            .withNetwork(network)
            .withNetworkAliases("ts-user-service")
            .dependsOn(userServiceMongoDBContainer);

    @Container
    private static GenericContainer<?> ticketinfoServiceContainer = new GenericContainer<>(
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
    public static MongoDBContainer trainServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-train-mongo");

    @Container
    private static GenericContainer<?> trainServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-train-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(14567)
            .withNetwork(network)
            .withNetworkAliases("ts-train-service")
            .dependsOn(trainServiceMongoDBContainer);

    @Container
    public static MongoDBContainer routeServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-route-mongo");

    @Container
    private static GenericContainer<?> routeServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-route-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(11178)
            .withNetwork(network)
            .withNetworkAliases("ts-route-service")
            .dependsOn(routeServiceMongoDBContainer);

    @Container
    public static MongoDBContainer priceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-price-mongo");

    @Container
    private static GenericContainer<?> priceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16579)
            .withNetwork(network)
            .withNetworkAliases("ts-price-service")
            .dependsOn(priceServiceMongoDBContainer);

    @Container
    public static MongoDBContainer consignPriceServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-mongo");

    @Container
    private static GenericContainer<?> consignPriceServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-consign-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16110)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-service")
            .dependsOn(consignPriceServiceMongoDBContainer);

    @Container
    public static MongoDBContainer configServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:latest"))
            .withNetwork(network)
            .withNetworkAliases("ts-config-mongo");

    @Container
    private static GenericContainer<?> configServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-config-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(15679)
            .withNetwork(network)
            .withNetworkAliases("ts-config-service")
            .dependsOn(configServiceMongoDBContainer);

    @RegisterExtension
    static WireMockExtension notificationServiceWireMock = WireMockExtension.newInstance().options(wireMockConfig().port(17853)).build();

    @BeforeAll
    public static void setUp() {
        configureFor("localhost", 17853);
        securityServiceContainer.start();
        orderOtherServiceContainer.start();
        orderServiceContainer.start();
        contactsServiceContainer.start();
        travel2ServiceContainer.start();
        ticketinfoServiceContainer.start();
        basicServiceContainer.start();
        stationServiceContainer.start();
        priceServiceContainer.start();
        routeServiceContainer.start();
        consignServiceContainer.start();
        consignPriceServiceContainer.start();
        foodServiceContainer.start();
        seatServiceContainer.start();
        userServiceContainer.start();
        configServiceContainer.start();
        assuranceServiceContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.assurance.service.url", assuranceServiceContainer::getHost);
        registry.add("ts.assurance.service.port", () -> assuranceServiceContainer.getMappedPort(18888));
        registry.add("ts.consign.service.url", consignServiceContainer::getHost);
        registry.add("ts.consign.service.port", () -> consignServiceContainer.getMappedPort(16111));
        registry.add("ts.travel2.service.url", travel2ServiceContainer::getHost);
        registry.add("ts.travel2.service.port", () -> travel2ServiceContainer.getMappedPort(16346));
        registry.add("ts.food.service.url", foodServiceContainer::getHost);
        registry.add("ts.food.service.port", () -> foodServiceContainer.getMappedPort(18856));
        registry.add("ts.seat.service.url", seatServiceContainer::getHost);
        registry.add("ts.seat.service.port", () -> seatServiceContainer.getMappedPort(18898));
        registry.add("ts.station.service.url", stationServiceContainer::getHost);
        registry.add("ts.station.service.port", () -> stationServiceContainer.getMappedPort(12345));
        registry.add("ts.contacts.service.url", contactsServiceContainer::getHost);
        registry.add("ts.contacts.service.port", () -> contactsServiceContainer.getMappedPort(12347));
        registry.add("ts.security.service.url", securityServiceContainer::getHost);
        registry.add("ts.security.service.port", () -> securityServiceContainer.getMappedPort(11188));
        registry.add("ts.user.service.url", userServiceContainer::getHost);
        registry.add("ts.user.service.port", () -> userServiceContainer.getMappedPort(12342));
        registry.add("ts.notification.service.url", () -> "localhost");
        registry.add("ts.notification.service.port", () -> "17853");
        registry.add("ts.ticketinfo.service.url", ticketinfoServiceContainer::getHost);
        registry.add("ts.ticketinfo.service.port", () -> ticketinfoServiceContainer.getMappedPort(15681));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
        registry.add("ts.basic.service.url", basicServiceContainer::getHost);
        registry.add("ts.basic.service.port", () -> basicServiceContainer.getMappedPort(15680));
        registry.add("ts.train.service.url", trainServiceContainer::getHost);
        registry.add("ts.train.service.port", () -> trainServiceContainer.getMappedPort(14567));
        registry.add("ts.route.service.url", routeServiceContainer::getHost);
        registry.add("ts.route.service.port", () -> routeServiceContainer.getMappedPort(11178));
        registry.add("ts.price.service.url", priceServiceContainer::getHost);
        registry.add("ts.price.service.port", () -> priceServiceContainer.getMappedPort(16579));
        registry.add("ts.consign.price.service.url", consignPriceServiceContainer::getHost);
        registry.add("ts.consing.price.service.port", () -> consignPriceServiceContainer.getMappedPort(16110));
        registry.add("ts.config.service.url", configServiceContainer::getHost);
        registry.add("ts.config.service.port", () -> configServiceContainer.getMappedPort(15679));
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the assurance type is invalid
     * <li><b>Parameters:</b></li> a valid request object with an assurance type != 0
     * <li><b>Expected result:</b></li> status 1, msg "Success.But Buy Assurance Fail.", data "Success"
     * <li><b>Related Issue:</b></li> <b>F22</b> The assurance service always returns status code 403 instead of 401
     * which leads to the fallback response being triggered instead.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void FAILING_testPreserveAssuranceInvalid() throws Exception {
        // Arrange
        mockNotificationService();
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406000000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":100," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.But Buy Assurance Fail.")))
                .andExpect(jsonPath("$.data", is("Success")));

        verifyMockNotificationService();
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request object is correct
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", data "Success"
     * <li><b>Related Issue:</b></li> <b>F16b:</b> The timeout value for the ts-preserve-other-service is set to 5000ms, which is
     * insufficient for a successful execution of their endpoints. Without increasing the timeout value, a timeout exception
     * will always be thrown. The timeout value was increased to 20000ms, as all test cases would fail otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void FIXED_PREVIOUSLY_FAILING_testPreserveCorrectObject() throws Exception {
        // Arrange
        mockNotificationService();
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":2," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"name\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is("Success")));

        verifyMockNotificationService();
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the accountId has the wrong format
     * <li><b>Parameters:</b></li> a valid request object with an accountId of "INVALID"
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testPreserveAccountIdInvalidFormat() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"INVALID\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the accountId does not exist
     * <li><b>Parameters:</b></li> a valid request object with an accountId that does not exist
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testPreserveAccountIdNonExisting() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-0000-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the contactsId is in the wrong format
     * <li><b>Parameters:</b></li> a valid request object with a contactsId of "INVALID"
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testPreserveContactsIdInvalidFormat() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"INVALID\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the contactsId does not exist
     * <li><b>Parameters:</b></li> a valid request object with a contactsId that does not exist
     * <li><b>Expected result:</b></li> status 0, msg "No contacts according to contacts id.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testPreserveContactsIdNonExisting() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-0000-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-0000-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No contacts according to contacts id")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId is in the wrong format
     * <li><b>Parameters:</b></li> a valid request object with a tripId of "INVALID"
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(7)
    public void testPreserveTripIdWrongFormat() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"INVALID\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is invalid
     * <li><b>Parameters:</b></li> a valid request object with a seatType of Integer.MIN_VALUE
     * <li><b>Expected result:</b></li> status 1, msg "Success.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(8)
    public void testPreserveSeatTypeInvalid() throws Exception {
        // Arrange
        mockNotificationService();
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d00000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":" + Integer.MIN_VALUE + "," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is("Success")));

        verifyMockNotificationService();
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are too few seats
     * <li><b>Parameters:</b></li> a valid request object with a seatType of 3
     * <li><b>Expected result:</b></li> status 0, msg "Seat Not Enough", no data
     * <li><b>Related Issue:</b></li> <b>F25c</b> Seattype is compared with the number of available seats, which does not make
     * sense logically and leads to errors, although "Seat Not Enough" should be returned.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(9)
    public void FAILING_testPreserveSeatTooFewSeats() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90001\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K8134\"," +
                        " \"seatType\":3," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Seat Not Enough")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the date is invalid
     * <li><b>Parameters:</b></li> a valid request object with a date that is not after today
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(10)
    public void testPreserveDateInvalidDate() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2000-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when from and to station names are invalid
     * <li><b>Parameters:</b></li> a valid request object with invalid station names for from and to
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(11)
    public void testPreserveFromToInvalidName() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"INVALID\"," +
                        " \"to\":\"INVALID\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no route between the start and end station
     * <li><b>Parameters:</b></li> a valid request object with a start and end station that do not have a route between them
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(12)
    public void testPreserveNoRoute() throws Exception {
        // Arrange
        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Jia Xing Nan\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
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
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when sending the email fails
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(13)
    public void testPreserveEmailError() throws Exception {
        // Arrange
        stubFor(WireMock.post(urlEqualTo("/api/v1/notifyservice/notification/preserve_success"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("false")));

        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68400000000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":0," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is("Success")));

        verifyMockNotificationService();
    }

    /**
     * <ul>
     * <li><b>called by ts-preserve-other-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available (including the notification service,
     * which will not be mocked in this case)
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(14)
    public void testPreserveUnavailableService() throws Exception {
        // Arrange
        orderServiceContainer.stop();
        stationServiceContainer.stop();
        travel2ServiceContainer.stop();
        assuranceServiceContainer.stop();
        consignServiceContainer.stop();
        foodServiceContainer.stop();
        contactsServiceContainer.stop();
        securityServiceContainer.stop();
        userServiceContainer.stop();
        ticketinfoServiceContainer.stop();
        basicServiceContainer.stop();
        trainServiceContainer.stop();
        routeServiceContainer.stop();
        priceServiceContainer.stop();
        consignPriceServiceContainer.stop();
        configServiceContainer.stop();

        String requestJson =
                "{\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d90000\"," +
                        " \"contactsId\":\"4d2a46c7-71cb-4cf1-a5bb-b68406d9da61\"," +
                        " \"tripId\":\"K1432\"," +
                        " \"seatType\":2," +
                        " \"date\":\"2026-01-01\"," +
                        " \"from\":\"Nan Jing\"," +
                        " \"to\":\"Shang Hai\"," +
                        " \"assurance\":0," +
                        " \"foodType\":2," +
                        " \"stationName\":\"station\"," +
                        " \"storeName\":\"store\"," +
                        " \"foodName\":\"food\"," +
                        " \"foodPrice\":5.99," +
                        " \"handleDate\":\"date\"," +
                        " \"consigneeName\":\"name\"," +
                        " \"consigneePhone\":\"911\"," +
                        " \"consigneeWeight\":75.82," +
                        " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveotherservice/preserveOther")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private void mockNotificationService() {
        stubFor(WireMock.post(urlEqualTo("/api/v1/notifyservice/notification/preserve_success"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));
    }

    private void verifyMockNotificationService() {
        verify(postRequestedFor(urlEqualTo("/api/v1/notifyservice/notification/preserve_success")));
    }
}
