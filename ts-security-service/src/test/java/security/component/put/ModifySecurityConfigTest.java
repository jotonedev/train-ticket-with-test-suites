package security.component.put;

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
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ModifySecurityConfigTest {

    @Autowired
    private SecurityRepository securityRepository;

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

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id of the SecurityConfig
     * in the request body
     * <li><b>Parameters:</b></li> database contains one element with the same id as the configuration in the request body
     * <li><b>Expected result:</b></li> status 1, msg "Success", the modified SecurityConfig
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifySecurityConfigElementInDatabase() throws Exception {
        // Arrange
        SecurityConfig storedConfig = populateDatabase();
        assertEquals(1L, securityRepository.count());

        SecurityConfig newConfig = new SecurityConfig();
        newConfig.setId(storedConfig.getId());
        newConfig.setName(UUID.randomUUID().toString());
        newConfig.setValue(UUID.randomUUID().toString());
        newConfig.setDescription(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newConfig)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.name", is(newConfig.getName())))
                .andExpect(jsonPath("$.data.value", is(newConfig.getValue())))
                .andExpect(jsonPath("$.data.description", is(newConfig.getDescription())));

        // Make sure that the SecurityConfig was updated in the database
        Optional<SecurityConfig> optionalSavedConfig = Optional.ofNullable(securityRepository.findByName(newConfig.getName()));
        assertTrue(optionalSavedConfig.isPresent());
        SecurityConfig savedConfig = optionalSavedConfig.get();
        assertEquals(newConfig.getName(), savedConfig.getName());
        assertEquals(newConfig.getValue(), savedConfig.getValue());
        assertEquals(newConfig.getDescription(), savedConfig.getDescription());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two SecurityConfig objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifySecurityConfigMultipleObjects() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson = "[{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}," +
                "{\"id\":\"" + id + "\", \"name\":\"name\", \"value\":\"1\", \"description\":\"sec\"}]";

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id of the SecurityConfig
     * <li><b>Parameters:</b></li> a SecurityConfig with a random id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Security Config Not Exist", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifySecurityConfigNotExistingObject() throws Exception {
        // Arrange
        assertEquals(0L, securityRepository.count());

        SecurityConfig newConfig = new SecurityConfig();
        newConfig.setId(UUID.randomUUID());
        newConfig.setName(UUID.randomUUID().toString());
        newConfig.setValue(UUID.randomUUID().toString());
        newConfig.setDescription(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newConfig)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Security Config Not Exist")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    void testModifySecurityConfigMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
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
    void testModifySecurityConfigMissingObject() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to modify a SecurityConfig object with null values for all attributes, since
     * name, value and description attributes of SecurityConfig are Strings with no restriction.
     * <li><b>Parameters:</b></li> An object with null values for all attributes
     * <li><b>Expected result:</b></li> status 1, "Success", the modified SecurityConfig now has null values for all attributes
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifySecurityConfigNullAttributes() throws Exception {
        // Arrange
        SecurityConfig storedConfig = populateDatabase();
        assertEquals(1L, securityRepository.count());

        SecurityConfig newConfig = new SecurityConfig();
        newConfig.setId(storedConfig.getId());

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newConfig)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.name", is(newConfig.getName())))
                .andExpect(jsonPath("$.data.value", is(newConfig.getValue())))
                .andExpect(jsonPath("$.data.description", is(newConfig.getDescription())));

        // Make sure that the SecurityConfig was updated in the database
        Optional<SecurityConfig> optionalSavedConfig = Optional.ofNullable(securityRepository.findByName(newConfig.getName()));
        assertTrue(optionalSavedConfig.isPresent());
        SecurityConfig savedConfig = optionalSavedConfig.get();
        assertNull(savedConfig.getName());
        assertNull(savedConfig.getValue());
        assertNull(savedConfig.getDescription());
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
    public void testModifySecurityConfigNullRequestBody() throws Exception {
        // Arrange
        String expectedError = "Required request body is missing";

        // Act
        mockMvc.perform(put("/api/v1/securityservice/securityConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    @BeforeEach
    void beforeEach() {
        securityRepository.deleteAll();
    }

    private SecurityConfig populateDatabase() {
        SecurityConfig config = new SecurityConfig();
        config.setId(UUID.randomUUID());
        config.setName(UUID.randomUUID().toString());
        return securityRepository.save(config);
    }
}
