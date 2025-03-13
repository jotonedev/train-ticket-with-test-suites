package security.integration.get;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CheckAccountIdTest {

    private final String ACCOUNT_ID = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";

    @Autowired
    SecurityRepository securityRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    public static final MongoDBContainer orderServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-mongo");

    @Container
    public static final MongoDBContainer orderOtherServiceMongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.0"))
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-mongo");

    @Container
    public static GenericContainer<?> orderServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12031)
            .withNetwork(network)
            .withNetworkAliases("ts-order-service")
            .dependsOn(orderServiceMongoDBContainer);

    @Container
    public static GenericContainer<?> orderOtherServiceContainer = new GenericContainer<>(
            DockerImageName.parse("local/ts-order-other-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12032)
            .withNetwork(network)
            .withNetworkAliases("ts-order-other-service")
            .dependsOn(orderOtherServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.order.service.url", orderServiceContainer::getHost);
        registry.add("ts.order.service.port", () -> orderServiceContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherServiceContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherServiceContainer.getMappedPort(12032));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the sum of both responses is lower than initialized value in both
     * orderInOneHour and validOrder
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testCheckCorrectId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", ACCOUNT_ID)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(ACCOUNT_ID)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services, two random UUIDs
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id. Only the first id in paths is used.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testCheckMultipleIds() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", ACCOUNT_ID, UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(ACCOUNT_ID)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when id cannot be found by the called services
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id. Since the called services cannot find
     * any orders with the provided id, the services return its default values (0)
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testCheckNonExistingId() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", id)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(id.toString())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the sum of both responses is higher than initialized value in both
     * orderInOneHour and validOrder
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services
     * <li><b>Expected result:</b></li> status 0, msg "Too much order in last one hour or too much valid order", the provided id.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testCheckTooHighValues() throws Exception {
        // Arrange
        UUID id = UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f");
        securityRepository.deleteAll();
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("-1");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("-1");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", ACCOUNT_ID)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Too much order in last one hour or too much valid order")))
                .andExpect(jsonPath("$.data", is(ACCOUNT_ID)));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the provided ID has no UUID format
     * <li><b>Parameters:</b></li> Some badly formatted string that does not follow the UUID format
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testCheckInvalidId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", "Does not follow UUID format")
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the securityConfigs named "max_order_1_hour" and "max_order_not_use" with the
     * configuration information do not exist in the repository.
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(6)
    public void testCheckNonExistingSecurityConfigs() throws Exception {
        // Arrange
        securityRepository.deleteAll();

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", ACCOUNT_ID)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-security-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> accountId as used in the initData of the order services
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(7)
    public void testCheckUnavailableService() throws Exception {
        // Arrange
        orderServiceContainer.stop();
        orderOtherServiceContainer.stop();

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", ACCOUNT_ID)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
