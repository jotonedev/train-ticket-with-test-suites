package user.component.delete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import user.entity.User;
import user.repository.UserRepository;
import user.service.impl.UserServiceImpl;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteUserByIdTest {

    @SpyBean
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or no element in the database matches the id given
     * in paths
     * <li><b>Parameters:</b></li> some random user id that is not in the database
     * <li><b>Expected result:</b></li> status 0, msg "USER NOT EXISTS", no data. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testDeleteUserByIdNoExist() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", UUID.randomUUID()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER NOT EXISTS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the database was unchanged
        assertEquals(0L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id given in paths
     * <li><b>Parameters:</b></li> a user id that is in the database
     * <li><b>Expected result:</b></li> status 1, msg "DELETE SUCCESS", no data. The user is no longer in the database.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testDeleteUserByIdElementInDatabase() throws Exception {
        // Arrange
        User user = populateDatabase();

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doNothing().when(userService).deleteUserAuth(Mockito.any(UUID.class), Mockito.any(HttpHeaders.class));

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", user.getUserId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the user was deleted from the database
        assertEquals(0L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id given in paths
     * <li><b>Parameters:</b></li> a user id that is in the database
     * <li><b>Expected result:</b></li> status 1, msg "DELETE SUCCESS", no data. The user is no longer in the database.
     * Only the first needed values are used.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testDeleteUserByIdMultipleId() throws Exception {
        // Arrange
        User user = populateDatabase();

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doNothing().when(userService).deleteUserAuth(Mockito.any(UUID.class), Mockito.any(HttpHeaders.class));

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", user.getUserId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the user was deleted from the database
        assertEquals(0L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty path variable
     * <li><b>Expected result:</b></li> an IlleglArgumentException
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testDeleteUserByIdMissingId() throws Exception {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/userservice/users/{userId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testDeleteUserByIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", UUID.randomUUID() + "/" + UUID.randomUUID())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private User populateDatabase() {
        return userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .userName(UUID.randomUUID().toString())
                .password(UUID.randomUUID().toString())
                .gender(1)
                .documentType(1)
                .documentNum(UUID.randomUUID().toString())
                .email(UUID.randomUUID().toString())
                .build());
    }
}
