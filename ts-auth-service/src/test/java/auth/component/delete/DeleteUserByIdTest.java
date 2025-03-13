package auth.component.delete;

import auth.entity.User;
import auth.repository.UserRepository;
import auth.security.jwt.JWTProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteUserByIdTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        userRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return a status code of forbidden when no Authorization
     * header is provided
     * <li><b>Parameters:</b></li> a userId that matches a user in the database
     * <li><b>Expected result:</b></li> a status code of 403, because the authorization is missing
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdNoAuthorization() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}", storedUser.getUserId()))
                .andDo(print())
                // Assert
                .andExpect(status().isForbidden());

        assertEquals(1L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return a status code of forbidden when Authorization is provided
     * but the user does not have the required role
     * <li><b>Parameters:</b></li> a userId that matches a user in the database
     * <li><b>Expected result:</b></li> a status code of 403, because the user does not have the required role
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdWrongAuthorization() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}", storedUser.getUserId())
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isForbidden());

        assertEquals(1L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return the users when the Authorization header is provided
     * and the user has the required role
     * <li><b>Parameters:</b></li> a userId that matches a user in the database
     * <li><b>Expected result:</b></li> status 1, msg "DELETE USER SUCCESS", no data. The user is deleted from the database.
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdCorrectAuthorizationUserCorrectlyDeleted() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithAdmin();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}", storedUser.getUserId())
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE USER SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the User was deleted from the database
        assertEquals(0L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return the users when the Authorization header is provided
     * and the user has the required role, but there is no user with the given id
     * <li><b>Parameters:</b></li> some random userId that does not match any user in the database
     * <li><b>Expected result:</b></li> status 1, msg "DELETE USER SUCCESS", no data. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>D1:</b> repository.delete() is performed without a check whether the object
     * exists, always resulting in a successful deletion.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdUserNotExists() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithAdmin();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}", UUID.randomUUID())
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE USER SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that nothing was deleted
        assertEquals(1L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when nothing is passed to the endpoint
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> an IllegalArgumentException
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/users/{userId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the id is not in the correct UUID format
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27c:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteUserByIdInvalidIdFormat() throws Exception {
        // Arrange
        String uuid = "Does not follow UUID format";
        String token = jwtProvider.createToken(generateAdmin());

        // Act
        mockMvc.perform(delete("/api/v1/users/{userId}", uuid)
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private User populateDatabaseWithAdmin() {
        User user = generateAdmin();
        return userRepository.save(user);
    }

    private User populateDatabaseWithUser() {
        User user = generateFdseUser();
        return userRepository.save(user);
    }

    private User generateAdmin() {
        User admin = User.builder()
                .userId(UUID.randomUUID())
                .username("admin")
                .password(passwordEncoder.encode("222222"))
                .roles(new HashSet<>(Arrays.asList("ROLE_ADMIN")))
                .build();
        return admin;
    }

    private User generateFdseUser() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username("fdse_microservice")
                .password(passwordEncoder.encode("111111"))
                .roles(new HashSet<>(Arrays.asList("ROLE_USER")))
                .build();
        return user;
    }

    private User configureUser(UUID id, String username, String password, Set<String> roles) {
        return new User(id, username, password, roles);
    }
}
