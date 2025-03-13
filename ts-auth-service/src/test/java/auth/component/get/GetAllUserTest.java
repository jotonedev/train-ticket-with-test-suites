package auth.component.get;

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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllUserTest {

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
     * <li><b>Parameters:</b></li> none (empty database)
     * <li><b>Expected result:</b></li> a status code of 403
     * @throws Exception
     */
    @Test
    public void testGetAllUserNoAuthorization() throws Exception {
        // Arrange
        assertEquals(0L, userRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/users"))
                .andDo(print())
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return a status code of forbidden when Authorization is provided
     * but the user does not have the required role
     * <li><b>Parameters:</b></li> none (database contains a user)
     * <li><b>Expected result:</b></li> a status code of 403
     * @throws Exception
     */
    @Test
    public void testGetAllUserWrongAuthorization() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return the users when the Authorization header is provided
     * and the user has the required role
     * <li><b>Parameters:</b></li> none (database contains a user)
     * <li><b>Expected result:</b></li> an array containing the saved users.
     * @throws Exception
     */
    @Test
    public void testGetAllUserCorrectAuthorizationElementInDatabase() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithAdmin();
        assertEquals(1L, userRepository.count());
        String token = jwtProvider.createToken(storedUser);

        // Act
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("[0].userId", is(storedUser.getUserId().toString())))
                .andExpect(jsonPath("[0].username", is(storedUser.getUsername())))
                .andExpect(jsonPath("[0].password", is(storedUser.getPassword())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can correctly return the users when the Authorization header is provided
     * and the user has the required role
     * <li><b>Parameters:</b></li> none (database is empty)
     * <li><b>Expected result:</b></li> an array containing the saved users.
     * @throws Exception
     */
    @Test
    public void testGetAllUserCorrectAuthorizationEmptyDatabase() throws Exception {
        // Arrange
        String token = jwtProvider.createToken(generateAdmin());

        // Act
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer "+ token))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
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
