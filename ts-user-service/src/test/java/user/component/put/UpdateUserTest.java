package user.component.put;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import user.entity.User;
import user.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateUserTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or there is no element in the database that matches
     * the username of userDto in the request body
     * <li><b>Parameters:</b></li> a configured user object with a username that is not in the database (empty database)
     * <li><b>Expected result:</b></li> status 0, msg "USER NOT EXISTS", no data.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateUserNotExist() throws Exception {
        // Arrange
        User user = configureUser();

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("USER NOT EXISTS")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the username of userDto
     * in the request body
     * <li><b>Parameters:</b></li> a configured user object with a username that is in the database
     * <li><b>Expected result:</b></li> status 1, msg "SAVE USER SUCCESS", the updated user object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateUserElementInDatabase() throws Exception {
        // Arrange
        User storedUser = populateDatabase();

        User updatedUser = configureUser();
        updatedUser.setUserName(storedUser.getUserName());

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedUser)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("SAVE USER SUCCESS")))
                .andExpect(jsonPath("$.data.userId", is(storedUser.getUserId().toString())))
                .andExpect(jsonPath("$.data.userName", is(storedUser.getUserName())))
                .andExpect(jsonPath("$.data.password", is(updatedUser.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(updatedUser.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(updatedUser.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(updatedUser.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(updatedUser.getEmail())));

        // Make sure the user was updated in the database
        assertEquals(1L, userRepository.count());
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUserName(storedUser.getUserName()));
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();

        // userId should be of old user
        assertEquals(storedUser.getUserId(), user.getUserId());

        // rest is of updated user
        assertEquals(updatedUser.getUserName(), user.getUserName());
        assertEquals(updatedUser.getPassword(), user.getPassword());
        assertEquals(updatedUser.getGender(), user.getGender());
        assertEquals(updatedUser.getDocumentType(), user.getDocumentType());
        assertEquals(updatedUser.getDocumentNum(), user.getDocumentNum());
        assertEquals(updatedUser.getEmail(), user.getEmail());
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
    public void testUpdateUserMultipleObjects() throws Exception {
        // Arrange
        User storedUser = populateDatabase();

        User updatedUser = configureUser();
        updatedUser.setUserName(storedUser.getUserName());

        String requestJson = new ObjectMapper().writeValueAsString(new User[]{updatedUser, updatedUser});

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
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
    public void testUpdateUserMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
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
    void testUpdateUserMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
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
    public void testUpdateUserNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
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
