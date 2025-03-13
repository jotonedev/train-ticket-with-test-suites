package user.component.post;

import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import user.dto.AuthDto;
import user.entity.User;
import user.repository.UserRepository;
import user.service.impl.UserServiceImpl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RegisterUserTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the userDto given in the request body contains a username that is already
     * in the database of users
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a configured user object with a username that is already in the database
     * <li><b>Expected result:</b></li> status 0, msg "USER HAS ALREADY EXISTS", no data. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testSaveUserUsernameAlreadyExists() throws Exception {
        // Arrange
        User user = populateDatabase();

        User newUser = configureUser();
        newUser.setUserName(user.getUserName());

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newUser)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER HAS ALREADY EXISTS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the database was unchanged
        assertEquals(1L, userRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the userDto given in the request body contains a username that does not
     * yet exist in the database of users
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a configured user object with a username that is not yet in the database
     * <li><b>Expected result:</b></li> status 1, msg "REGISTER USER SUCCESS", the saved user. The user is saved in the database.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testSaveUserUsernameDoesNotExist() throws Exception {
        // Arrange
        User user = configureUser();

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doReturn(new Response<>()).when(userService).createDefaultAuthUser(Mockito.any(AuthDto.class));

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
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

        // Make sure the user was saved in the database
        assertEquals(1L, userRepository.count());
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUserName(user.getUserName()));
        assertTrue(userOptional.isPresent());
        User savedUser = userOptional.get();
        assertEquals(savedUser.getUserId(), user.getUserId());
        assertEquals(savedUser.getUserName(), user.getUserName());
        assertEquals(savedUser.getPassword(), user.getPassword());
        assertEquals(savedUser.getGender(), user.getGender());
        assertEquals(savedUser.getDocumentType(), user.getDocumentType());
        assertEquals(savedUser.getDocumentNum(), user.getDocumentNum());
        assertEquals(savedUser.getEmail(), user.getEmail());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two User objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSaveUserMultipleObjects() throws Exception {
        // Arrange
        User user = configureUser();
        String requestJson = new ObjectMapper().writeValueAsString(new User[]{user, user});

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSaveUserMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    void testSaveUserMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSaveUserRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> Test: how the endpoint behaves when the userDto given in the request body contains a username that does not
     * yet exist in the database of users, but the userId within the userDto is null
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a configured user object with a username that is not yet in the database and a null userId
     * <li><b>Expected result:</b></li> status 1, msg "REGISTER USER SUCCESS", the saved user. The service generates a random
     * userId and assigns it to the new user, which is created and saved in the database.
     * <li><b>Related Issue:</b></li> <b>F11:</b> There is a mismatch of return types : fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testSaveUserUsernameDoesNotExistIdNull() throws Exception {
        // Arrange
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

        User user = configureUser();
        user.setUserId(null);

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doReturn(new Response<>()).when(userService).createDefaultAuthUser(Mockito.any(AuthDto.class));

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("REGISTER USER SUCCESS")))

                // Check if a userId is set and matches the UUID regex
                .andExpect(jsonPath("$.data.userId", notNullValue()))
                .andExpect(jsonPath("$.data.userId", matchesPattern(uuidRegex)))

                .andExpect(jsonPath("$.data.userName", is(user.getUserName())))
                .andExpect(jsonPath("$.data.password", is(user.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(user.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(user.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(user.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));

        // Make sure the user was saved in the database
        assertEquals(1L, userRepository.count());
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUserName(user.getUserName()));
        assertTrue(userOptional.isPresent());
        User savedUser = userOptional.get();

        // Check if a userId is set and matches the UUID regex
        assertNotNull(savedUser.getUserId());
        assertTrue(savedUser.getUserId().toString().matches(uuidRegex));

        assertEquals(savedUser.getUserName(), user.getUserName());
        assertEquals(savedUser.getPassword(), user.getPassword());
        assertEquals(savedUser.getGender(), user.getGender());
        assertEquals(savedUser.getDocumentType(), user.getDocumentType());
        assertEquals(savedUser.getDocumentNum(), user.getDocumentNum());
        assertEquals(savedUser.getEmail(), user.getEmail());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> Test: how the endpoint behaves when the userDto given in the request body contains a username that does not
     * yet exist in the database of users, but the userId within the userDto already exists in the database
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a configured user object with a username that is not yet in the database and a userId that is already
     * in the database
     * <li><b>Expected result:</b></li> status 1, msg "REGISTER USER SUCCESS", the saved user. The user is added to the database
     * <li><b>Related Issue:</b></li> <b>D3:</b> A user is made unique by its username. Therefore, in the unlikely (but possible) event
     * where someone tries to create a user with an ID that already exists, it will be allowed as long as the username is unique.
     * If handled correctly, this won't cause any issues, but is an odd design decision nonetheless, because the purpose of an ID is to
     * identify the user.<br>
     * <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testSaveUserIdAlreadyExists() throws Exception {
        // Arrange
        User user = populateDatabase();

        User newUser = configureUser();
        // Set the userId to the userId of the user in the database
        newUser.setUserId(user.getUserId());

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doReturn(new Response<>()).when(userService).createDefaultAuthUser(Mockito.any(AuthDto.class));

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newUser)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("REGISTER USER SUCCESS")))
                .andExpect(jsonPath("$.data.userId", is(user.getUserId().toString())))
                .andExpect(jsonPath("$.data.userName", is(newUser.getUserName())))
                .andExpect(jsonPath("$.data.password", is(newUser.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(newUser.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(newUser.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(newUser.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(newUser.getEmail())));

        // Make sure the user was saved in the database
        assertEquals(2L, userRepository.count());
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUserName(newUser.getUserName()));
        assertTrue(userOptional.isPresent());
        User savedUser = userOptional.get();
        assertEquals(savedUser.getUserId(), newUser.getUserId());
        assertEquals(savedUser.getUserName(), newUser.getUserName());
        assertEquals(savedUser.getPassword(), newUser.getPassword());
        assertEquals(savedUser.getGender(), newUser.getGender());
        assertEquals(savedUser.getDocumentType(), newUser.getDocumentType());
        assertEquals(savedUser.getDocumentNum(), newUser.getDocumentNum());
        assertEquals(savedUser.getEmail(), newUser.getEmail());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> Test: how the endpoint behaves when the userDto given in the request body contains a userId that does not
     * yet exist in the database of users, but the username within the userDto already exists in the database
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-auth-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a configured user object with a userId that is not yet in the database and a username that is already
     * in the database
     * <li><b>Expected result:</b></li> status 0, msg "USER HAS ALREADY EXISTS", no data. The database remains unchanged
     * <li><b>Related Issue:</b></li> <b>D3:</b> A user is made unique by its username. Therefore, in the unlikely (but possible) event
     * where someone tries to create a user with an ID that already exists, it will be allowed as long as the username is unique.
     * If handled correctly, this won't cause any issues, but is an odd design decision nonetheless, because the purpose of an ID is to
     * identify the user.<br>
     * <b>F11:</b> There is a mismatch of return types: fallback method in
     * UserController returns {@code HttpEntity} and the return type of this controller method registerUser returns {@code ResponseEntity<Response>},
     * which leads to a compiler error when attempting to execute this service. The return type of the fallback method
     * was adjusted to {@code ResponseEntity<Response>}, as the test cases and the service would not be executable otherwise.
     * That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testSaveUserNameAlreadyExists() throws Exception {
        // Arrange
        User user = populateDatabase();

        User newUser = configureUser();
        // Set the username to the username of the user in the database
        newUser.setUserName(user.getUserName());

        // authservice will be called, but since nothing is done with the response of the call, we can just
        // specify that the call should do nothing.
        Mockito.doReturn(new Response<>()).when(userService).createDefaultAuthUser(Mockito.any(AuthDto.class));

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newUser)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER HAS ALREADY EXISTS")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure the database was unchanged
        assertEquals(1L, userRepository.count());
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
