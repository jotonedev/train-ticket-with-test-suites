package auth.component.post;

import auth.dto.AuthDto;
import auth.repository.UserRepository;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateDefaultAuthUserTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with a valid id, username and password
     * <li><b>Parameters:</b></li> an AuthDto with a valid id, username and password
     * <li><b>Expected result:</b></li> status 1, msg "Success", the created user. The database contains the new User
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateDefaultAuthUserCorrectObject() throws Exception {
        // Arrange
        AuthDto authDto = configueAuthDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("SUCCESS")))
                .andExpect(jsonPath("$.data.userId", is(authDto.getUserId())))
                .andExpect(jsonPath("$.data.userName", is(authDto.getUserName())))
                .andExpect(jsonPath("$.data.password", is(authDto.getPassword())));

        // Make sure that the user was created in the database
        Assertions.assertEquals(userRepository.findAll().size(), 1);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two AuthDto objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateDefaultAuthUserMultipleObjects() throws Exception {
        // Arrange
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(configueAuthDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        jsonArray.add(configueAuthDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
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
    public void testCreateDefaultAuthUserMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/auth")
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
    public void testCreateDefaultAuthUserMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/auth")
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
    public void testCreateDefaultAuthUserNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with an empty username
     * <li><b>Parameters:</b></li> an AuthDto with a valid id and password, but an empty username
     * <li><b>Expected result:</b></li> an Exception with the message "username cannot be empty."
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateDefaultAuthUserUsernameEmpty() throws Exception {
        // Arrange
        AuthDto authDto = configueAuthDto(UUID.randomUUID().toString(), "", UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                // Assert
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[username cannot be empty.]"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with null value for the username
     * <li><b>Parameters:</b></li> an AuthDto with a valid id and password, but null username
     * <li><b>Expected result:</b></li> an Exception with the message "username cannot be empty."
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateDefaultAuthUserUsernameNull() throws Exception {
        // Arrange
        AuthDto authDto = configueAuthDto(UUID.randomUUID().toString(), null, UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                // Assert
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[username cannot be empty.]"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with an empty password
     * <li><b>Parameters:</b></li> an AuthDto with a valid id and username, but empty password
     * <li><b>Expected result:</b></li> an Exception with the message "Passwords must contain at least 6 characters."
     * <li><b>Related Issue:</b></li> <b>F2:</b> The ts-auth-service encodes the provided password using
     * {@code .password(passwordEncoder.encode(dto.Password)))} and after, checks the length of the now encoded password.
     * Since the encoded password always meets the criteria of being at least six characters long, the error of having
     * a password that is too short can never be reached.
     * </ul>
     */
    @Test
    public void FAILING_testCreateDefaultAuthUserPasswordTooShort() throws Exception {
        // Arrange
        AuthDto authDto = configueAuthDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "");

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                // Assert
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("[Passwords must contain at least 6 characters.]"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with null value for the password
     * <li><b>Parameters:</b></li> an AuthDto with a valid id and username, but null password
     * <li><b>Expected result:</b></li> a NestedServletException with the message "rawPassword cannot be null"
     * </ul>
     */
    @Test
    public void testCreateDefaultAuthUserPasswordNull() {
        // Arrange
        AuthDto authDto = configueAuthDto(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);

        Exception exception = assertThrows(NestedServletException.class,
                // Act
                () -> mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto))));

        // Assert
        assertTrue(Objects.requireNonNull(exception.getMessage()).contains("rawPassword cannot be null"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an AuthDto with null value for the password
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27c:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateDefaultAuthUserIncorrectUUIDFormat() throws Exception {
        // Arrange
        String uuid = "Not a UUID format";
        AuthDto authDto = configueAuthDto(uuid, UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(authDto)))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private AuthDto configueAuthDto(String id, String username, String password) {
        return new AuthDto(id, username, password);
    }
}
