package auth.integration.post;

import auth.dto.BasicAuthDto;
import auth.entity.User;
import auth.repository.UserRepository;
import com.alibaba.fastjson.JSONObject;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    private final static String BASEURL = "/api/v1/users";

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    /*
     * Because the external ts-verification-code-service uses Cookie and Cache, which we can't influence or replicate via tests,
     * we mock the service via WireMock. WireMock allows to simulate the communication between the ts-auth-service and the
     * ts-verification-service via a mocked http server with mocked responses.
     */
    @RegisterExtension
    static WireMockExtension verificationCodeServiceWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(15678)).build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl());
        registry.add("ts.verification.code.service.url", () -> "localhost");
        registry.add("ts.verification.code.service.port",() -> "15678");
    }

    @BeforeAll
    static void setupWireMock() {
        configureFor("localhost", 15678);
    }

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when a correct BasicAuthDto object is posted
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-validation-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, and with a verification code that is valid
     * <li><b>Expected result:</b></li> status 1, msg "login success", a TokenDto abject with the user's id, username, and a token
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserTestCorrectObject() throws Exception {
        // Arrange
        User user = configureUser(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        BasicAuthDto basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername(user.getUsername());
        basicAuthDto.setPassword(user.getPassword());
        basicAuthDto.setVerificationCode("verificationCode");

        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockVerificationCodeService();

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("login success")))
                .andExpect(jsonPath("$.data.userId", is(user.getUserId().toString())))
                .andExpect(jsonPath("$.data.username", is(user.getUsername())))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        verifyMockVerificationCodeService();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no user with the given username in the repository
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-validation-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, and with a verification code that is valid,
     * but the user is not in the repository
     * <li><b>Expected result:</b></li> status 0, msg "Verification failed.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserTestUserNotInRepository() throws Exception {
        // Arrange
        User user = configureUser(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        BasicAuthDto basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername(user.getUsername());
        basicAuthDto.setPassword(user.getPassword());
        basicAuthDto.setVerificationCode("verificationCode");

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        mockVerificationCodeService();

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Verification failed.")))
                .andExpect(jsonPath("$.data", nullValue()));

        verifyMockVerificationCodeService();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the username or password is incorrect
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-validation-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with an incorrect username and password
     * <li><b>Expected result:</b></li> status 0, msg "Incorrect username or password.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserTestUsernamePasswordIncorrect() throws Exception {
        // Arrange
        BasicAuthDto basicAuthDto = new BasicAuthDto();
        basicAuthDto.setUsername("");
        basicAuthDto.setPassword("");
        basicAuthDto.setVerificationCode("verificationCode");

        JSONObject json = new JSONObject();
        json.put("username", basicAuthDto.getUsername());
        json.put("password", basicAuthDto.getPassword());
        json.put("verificationCode", basicAuthDto.getVerificationCode());

        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Incorrect username or password."));

        mockVerificationCodeService();

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Incorrect username or password.")))
                .andExpect(jsonPath("$.data", nullValue()));

        verifyMockVerificationCodeService();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-verification-code-service returns false for the verification code
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-validation-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, but with a verification code that is invalid
     * <li><b>Expected result:</b></li> status 0, msg "Verification failed.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserTestVerifyCodeInvalid() throws Exception {
        // Arrange
        User user = configureUser(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", user.getUsername());
        json.put("password", user.getPassword());

        json.put("verificationCode", "verificationCode");

        stubFor(get(urlEqualTo("/api/v1/verifycode/verify/verificationCode"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("false")));

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Verification failed.")))
                .andExpect(jsonPath("$.data", nullValue()));

        verifyMockVerificationCodeService();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ts-verification-code-service is unavailable
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-validation-code-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a BasicAuthDto with a username and correct password, but the verification code service is unavailable
     * <li><b>Expected result:</b></li> status 0, msg "Verification failed.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testLoginUserTestServiceUnavailable() throws Exception {
        // Assert
        User user = configureUser(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        userRepository.save(user);

        JSONObject json = new JSONObject();
        json.put("username", user.getUsername());
        json.put("password", user.getPassword());
        json.put("verificationCode", UUID.randomUUID().toString());

        // Act
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/login")
                        .content(json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Verification failed.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private void mockVerificationCodeService() {
        stubFor(get(urlEqualTo("/api/v1/verifycode/verify/verificationCode"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));
    }

    private void verifyMockVerificationCodeService() {
        verify(getRequestedFor(urlEqualTo("/api/v1/verifycode/verify/verificationCode")));
    }

    private User configureUser(String username, String password) {
        return new User(UUID.randomUUID(), username, password, new HashSet<>(Arrays.asList("ROLE_USER")));
    }
}
