package user.integration.delete;

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
import user.entity.User;
import user.repository.UserRepository;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeleteUserByIdTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    public static MongoDBContainer authServiceMongoDbContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"))
            .withExposedPorts(27017)
            .withNetwork(network)
            .withNetworkAliases("ts-auth-mongo");

    // requires docker image of ts-auth-service with the name local/ts-auth-service:0.1
    @Container
    public static GenericContainer<?> authServiceContainer = new GenericContainer<>(DockerImageName.parse("local/ts-auth-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(12340)
            .withNetwork(network)
            .withNetworkAliases("ts-auth-service")
            .dependsOn(authServiceMongoDbContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.auth.service.url", authServiceContainer::getHost);
        registry.add("ts.auth.service.port", () -> authServiceContainer.getMappedPort(12340));
    }

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide a userId that exists in the database
     * <li><b>Parameters:</b></li> A userId that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "DELETE SUCCESS", no data. The database should have one less element.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testDeleteUserByIdElementInDatabase() throws Exception {
        // Arrange
        long initialSize = userRepository.count();

        User user = configureUser();
        userRepository.save(user);

        assertEquals(1, userRepository.count());

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", user.getUserId())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the user was removed from the database
        assertEquals(0, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide a userId that does not exist in the database
     * <li><b>Parameters:</b></li> Some random userId that is not in the database
     * <li><b>Expected result:</b></li> status 0, msg "USER NOT EXISTS", no data. The database size should remain the same.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testDeleteUserByIdMissingObject() throws Exception {
        // Arrange
        long initialSize = userRepository.count();

        UUID id = UUID.randomUUID();

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", id.toString())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER NOT EXISTS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the database size remains the same
        assertEquals(initialSize, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testDeleteUserByIdUnavailableService() throws Exception {
        // Arrange
        User user = configureUser();
        userRepository.save(user);

        authServiceContainer.stop();

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", user.getUserId())
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private User configureUser() {
        return User.builder()
                .userId(UUID.randomUUID())
                .userName(UUID.randomUUID().toString())
                .password(UUID.randomUUID().toString())
                .gender(1)
                .documentType(1)
                .documentNum(UUID.randomUUID().toString())
                .email(UUID.randomUUID().toString())
                .build();
    }
}
