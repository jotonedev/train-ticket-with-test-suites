package auth.component.post;

import auth.dto.BasicAuthDto;
import auth.entity.User;
import auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginUserTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide a BasicAuthDto with a username and correct password, but without
     * a verification code
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, but without a verification code
     * <li><b>Expected result:</b></li> status 1, msg "login success", a TokenDto abject with the user's id, username, and a token
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserNoVerificationCode() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        BasicAuthDto basicAuthDto = transformUserToBasicAuthDtoWithVerificationCode(storedUser);
        basicAuthDto.setPassword("111111");
        assertEquals(1L, userRepository.count());

        // Act
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(basicAuthDto)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("login success")))
                .andExpect(jsonPath("$.data.userId", is(storedUser.getUserId().toString())))
                .andExpect(jsonPath("$.data.username", is(storedUser.getUsername())))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide a BasicAuthDto with a username and correct password, and
     * with a verification code that is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-verification-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, and with a verification code that however fails
     * <li><b>Expected result:</b></li> status 0, msg "verification failed", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserVerificationCodeInvalid() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        BasicAuthDto basicAuthDto = transformUserToBasicAuthDtoWithVerificationCode(storedUser);
        String verificationCode = "invalid verification code";
        basicAuthDto.setVerificationCode(verificationCode);
        basicAuthDto.setPassword("111111");
        assertEquals(1L, userRepository.count());

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-verification-code-service:15678/api/v1/verifycode/verify/" + verificationCode),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Boolean>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(basicAuthDto)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Verification failed.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide a BasicAuthDto with a username and correct password, and
     * with a verification code that is valid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-verification-code-service</li>
     *   </ul>
         * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, and with a verification code that is valid
     * <li><b>Expected result:</b></li> status 1, msg "login success", a TokenDto abject with the user's id, username, and a token
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserVerificationServiceSuccessful() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        BasicAuthDto basicAuthDto = transformUserToBasicAuthDtoWithVerificationCode(storedUser);
        String verificationCode = "valid verification code";
        basicAuthDto.setVerificationCode(verificationCode);
        basicAuthDto.setPassword("111111");
        assertEquals(1L, userRepository.count());

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-verification-code-service:15678/api/v1/verifycode/verify/" + verificationCode),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Boolean>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(basicAuthDto)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("login success")))
                .andExpect(jsonPath("$.data.userId", is(storedUser.getUserId().toString())))
                .andExpect(jsonPath("$.data.username", is(storedUser.getUsername())))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the called ts-verification-code-service raises an error and returns null
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-verification-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, and with a verification code that however fails
     * <li><b>Expected result:</b></li> status 0, msg "verification failed", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserVerificationServiceError() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        BasicAuthDto basicAuthDto = transformUserToBasicAuthDtoWithVerificationCode(storedUser);
        String verificationCode = "error verification code";
        basicAuthDto.setVerificationCode(verificationCode);
        assertEquals(1L, userRepository.count());

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-verification-code-service:15678/api/v1/verifycode/verify/" + verificationCode),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Boolean>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(basicAuthDto)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Verification failed.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the password is incorrect
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and an incorrect password
     * <li><b>Expected result:</b></li> status 0, msg "Incorrect username or password", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserIncorrectPassword() throws Exception {
        // Arrange
        User storedUser = populateDatabaseWithUser();
        BasicAuthDto basicAuthDto = transformUserToBasicAuthDtoWithVerificationCode(storedUser);
        basicAuthDto.setPassword("wrong password");
        assertEquals(1L, userRepository.count());

        // Act
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(basicAuthDto)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Incorrect username or password.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two BasicAuthDto objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserMultipleObjects() throws Exception {
        // Arrange
        List<BasicAuthDto> basicAuthDtos = Arrays.asList(
                transformUserToBasicAuthDtoWithVerificationCode(generateFdseUser()),
                transformUserToBasicAuthDtoWithVerificationCode(generateFdseUser())
        );
        String requestJson = new ObjectMapper().writeValueAsString(basicAuthDtos);

        // Act
        mockMvc.perform(post("/api/v1/users/login")
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
    public void testLoginUserMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/users/login")
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
    void testLoginUserMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/users/login")
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
    public void testLoginUserNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private BasicAuthDto transformUserToBasicAuthDtoWithVerificationCode(User user) {
        return new BasicAuthDto(user.getUsername(), user.getPassword(), "");
    }

    private User populateDatabaseWithUser() {
        User user = generateFdseUser();
        return userRepository.save(user);
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
}
