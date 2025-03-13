package config.component.post;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import config.entity.Config;
import config.repository.ConfigRepository;
import org.junit.jupiter.api.Assertions;
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

import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateConfigTest {

    @Autowired
    private ConfigRepository configRepository;

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
        configRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Config that does not yet exist in the database
     * <li><b>Parameters:</b></li> A config with a name that does not exist in the database
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created config. The database contains the new config
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateConfigCorrectObject() throws Exception {
        // Arrange
        Config config = prepareConfig();

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config)))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(config.getValue())))
                .andExpect(jsonPath("$.data.description", is(config.getDescription())));

        // Make sure the config is stored in the database
        Assertions.assertEquals(1, configRepository.count());
        Config updatedConfig = configRepository.findByName(config.getName());
        Assertions.assertEquals(config.getValue(), updatedConfig.getValue());
        Assertions.assertEquals(config.getDescription(), updatedConfig.getDescription());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Config that already exists in the database
     * <li><b>Parameters:</b></li> a Config with a name that matches the id in the database
     * <li><b>Expected result:</b></li> status 0, msg "Config " + name + " already exists", the provided Station. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateConfigDuplicativeObject() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config)))
                // Assert
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Config " + config.getName() + " already exists.")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Config objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateConfigMultipleObjects() throws Exception {
        // Arrange
        Config config = prepareConfig();

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the config is created with empty values.
     * <li><b>Parameters:</b></li> a config with an empty string for name, value and description
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created config. Name, value, and description are empty strings
     * without any restrictions, apart from the value and name which cannot be null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateConfigEmptyValues() throws Exception {
        // Arrange
        Config newConfig = new Config("", "", "");

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(newConfig)))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.name", is("")))
                .andExpect(jsonPath("$.data.value", is("")))
                .andExpect(jsonPath("$.data.description", is("")));

        // Make sure the config is stored in the database
        Assertions.assertEquals(1, configRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the config is created with null values.
     * <li><b>Parameters:</b></li> a config with null values for name, value and description
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created config. Name, value, and description are empty strings
     * without any restrictions, apart from the value and name which cannot be null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateConfigNullValues() throws Exception {
        // Arrange
        Config newConfig = new Config(null, null, null);

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(newConfig)))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.name", is("")))
                .andExpect(jsonPath("$.data.value", is("")))
                .andExpect(jsonPath("$.data.description", is(nullValue())));

        // Make sure the config is stored in the database
        Assertions.assertEquals(1, configRepository.count());
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
    public void testCreateConfigMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
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
    public void testCreateConfigMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
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
    public void testCreateConfigNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private Config prepareConfig() {
        return new Config(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }
}
