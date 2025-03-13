package execute.integration.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TicketExecuteTest {

    private final static Network network = Network.newNetwork();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;


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


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ts.order.service.url", orderContainer::getHost);
        registry.add("ts.order.service.port", () -> orderContainer.getMappedPort(12031));
        registry.add("ts.order.other.service.url", orderOtherContainer::getHost);
        registry.add("ts.order.other.service.port", () -> orderOtherContainer.getMappedPort(12032));
    }

    /**
     * <ul>
     * <li><b>Called by ts-execute-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an existing ID as the URL parameter that can be found by
     * the ts-order-service.
     * <li><b>Parameters:</b></li> an orderId that can be found by the ts-order-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testTicketExecuteCorrectIdOrderService() throws  Exception {
        // Arrange
        String orderId = "d3c91694-d5b8-424c-8674-e14c89226e49";

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", orderId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-execute-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an orderID that cannot be found by the ts-order-service
     * but can be found by the ts-order-other-service.
     * <li><b>Parameters:</b></li> an orderId that can be found by the ts-order-other-service.
     * <li><b>Expected result:</b></li> status 1, msg "Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testTicketExecuteCorrectIdOrderOtherService() throws  Exception {
        // Arrange
        String orderId = "4d2a46c7-71cb-4cf1-a5bb-b68406d9da6e";

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", orderId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-execute-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an orderID that can not be found by the ts-order-service or
     * the ts-order-other-service.
     * <li><b>Parameters:</b></li> some random Id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testTicketExecuteIdNotExists() throws  Exception {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", orderId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-execute-service:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
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
    @Order(4)
    public void testTicketExecuteServiceUnavailable() throws Exception {
        // Arrange
        String orderId = "4d2a46c7-71cb-4cf1-a5bb-b68406d9da6e";

        orderContainer.stop();
        orderOtherContainer.stop();

        // Act
        mockMvc.perform(get("/api/v1/executeservice/execute/execute/{orderId}", orderId)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
