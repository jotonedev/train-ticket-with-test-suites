package user.integration.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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
import user.entity.User;
import user.repository.UserRepository;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegisterUserTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to save a user with valid attributes that does not already exist in the database
     * <li><b>Parameters:</b></li> a valid user object
     * <li><b>Expected result:</b></li> status 1, msg "REGISTER USER SUCCESS", the registered user object
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testRegisterUserCorrectObject() throws Exception {
        // Arrange
        User user = configureUser();

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("REGISTER USER SUCCESS")))
                .andExpect(jsonPath("$.data.userId", is(user.getUserId().toString())))
                .andExpect(jsonPath("$.data.userName", is(user.getUserName())))
                .andExpect(jsonPath("$.data.password", is(user.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(user.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(user.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(user.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));

        // check if the user is saved in the database
        assertNotNull(userRepository.findByUserId(user.getUserId()));
    }

    /**
     * <ul>
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to save a user with valid attributes that already exists in the database
     * <li><b>Parameters:</b></li> a valid user object that already exists in the database
     * <li><b>Expected result:</b></li> status 0, msg "USER HAS ALREADY EXISTS", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testRegisterUserUsernameAlreadyExists() throws Exception {
        // Arrange
        User user = configureUser();
        userRepository.save(user);

        // The user we request has the same username as the user we saved in the repository
        User userRequest = configureUser();
        userRequest.setUserName(user.getUserName());

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER HAS ALREADY EXISTS")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to save a user with an empty username
     * <li><b>Parameters:</b></li> a valid user object with an empty username
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testRegisterUserUsernameEmpty() throws Exception {
        // Arrange
        User user = configureUser();
        user.setUserName("");

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to save a user with a null username
     * <li><b>Parameters:</b></li> a valid user object with a null username
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testRegisterUserUsernameNull() throws Exception {
        // Arrange
        User user = configureUser();
        user.setUserName(null);

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
     * <li><b>Called by ts-user-service:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to save a user with a null username
     * <li><b>Parameters:</b></li> a valid user object with a null username
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>F2:</b> The ts-auth-service encodes the provided password using
     * {@code .password(passwordEncoder.encode(dto.Password)))} and after, checks the length of the now encoded password.
     * Since the encoded password always meets the criteria of being at least six characters long, the error of having
     * a password that is too short can never be reached.<br>
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void FAILING_testRegisterUserPasswordTooShort() throws Exception {
        // Arrange
        User user = configureUser();

        // Password needs to be at least 6 characters long. It can also not be empty or null.
        user.setPassword("12345");

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
    @Order(6)
    void testRegisterUserUnavailableService() throws Exception {
        User user = configureUser();

        authServiceContainer.stop();

        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
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
