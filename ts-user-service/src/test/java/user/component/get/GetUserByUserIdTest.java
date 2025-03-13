package user.component.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetUserByUserIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or there is no element in the database that
     * matches the username given in paths
     * <li><b>Parameters:</b></li> some random username that is not in the database
     * <li><b>Expected result:</b></li> status 0, msg "No User", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetUserByUserIdNotExist() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", UUID.randomUUID()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No User")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the username given in paths
     * <li><b>Parameters:</b></li> a username that is in the database
     * <li><b>Expected result:</b></li> status 1, msg "Find User Success", the found user.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetUserByUserIdElementInDatabase() throws Exception {
        User user = populateDatabase();

        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", user.getUserId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find User Success")))
                .andExpect(jsonPath("$.data.userId", is(user.getUserId().toString())))
                .andExpect(jsonPath("$.data.userName", is(user.getUserName())))
                .andExpect(jsonPath("$.data.password", is(user.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(user.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(user.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(user.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Find User Success", the found user. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetUserByUserIdMultipleId() throws Exception {
        // Arrange
        User user = populateDatabase();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", user.getUserId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find User Success")))
                .andExpect(jsonPath("$.data.userId", is(user.getUserId().toString())))
                .andExpect(jsonPath("$.data.userName", is(user.getUserName())))
                .andExpect(jsonPath("$.data.password", is(user.getPassword())))
                .andExpect(jsonPath("$.data.gender", is(user.getGender())))
                .andExpect(jsonPath("$.data.documentType", is(user.getDocumentType())))
                .andExpect(jsonPath("$.data.documentNum", is(user.getDocumentNum())))
                .andExpect(jsonPath("$.data.email", is(user.getEmail())));
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
    public void testGetUserByUserIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/userservice/users/id/{userId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetUserByUserIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString()))
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
