package config.component.put;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the name of the config given in requestBody
     * <li><b>Parameters:</b></li> a config with a name that matches the name in the database
     * <li><b>Expected result:</b></li> status 1, msg "update success", the updated config.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConfigCorrectObject() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);
        Config newConfig = new Config(config.getName(), "1.5", "updatedDescription");

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newConfig)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(newConfig.getValue())))
                .andExpect(jsonPath("$.data.description", is(newConfig.getDescription())));

        // Make sure the config is updated in the database
        Config updatedConfig = configRepository.findByName(config.getName());
        Assertions.assertEquals(newConfig.getValue(), updatedConfig.getValue());
        Assertions.assertEquals(newConfig.getDescription(), updatedConfig.getDescription());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the name given in requestBody
     * <li><b>Parameters:</b></li> a config with a random name that does not exist in the database
     * <li><b>Expected result:</b></li> status 0-, msg "Config" + name + "doesn't exist", no data. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConfigNotExists() throws Exception {
        // Arrange
        Config config = prepareConfig();
        config.setName(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config)))
                // Assert
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Config " + config.getName() + " doesn't exist.")))
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
    public void testUpdateConfigMultipleObjects() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(config);
        jsonArray.add(config);

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray.toJSONString()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the config is updated with empty values.
     * <li><b>Parameters:</b></li> a config with a name that matches the name in the database and empty strings for value and description
     * <li><b>Expected result:</b></li> status 1, msg "update success", the updated config. The value and description are empty strings
     * without any restrictions, apart from the value which cannot be null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConfigEmptyValues() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        Config newConfig = new Config(config.getName(), "", "");

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(newConfig)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.name", is(newConfig.getName())))
                .andExpect(jsonPath("$.data.value", is("")))
                .andExpect(jsonPath("$.data.description", is("")));

        // Make sure the config is updated in the database
        Config updatedConfig = configRepository.findByName(config.getName());
        Assertions.assertEquals(newConfig.getValue(), updatedConfig.getValue());
        Assertions.assertEquals(newConfig.getDescription(), updatedConfig.getDescription());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the config is updated with null values.
     * <li><b>Parameters:</b></li> a config with a name that matches the name in the database and null values for value and description
     * <li><b>Expected result:</b></li> status 1, msg "update success", the updated config. The description and value are strings
     * without restrictions, apart form the fact that the value cannot be null, so it is set to an empty string.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConfigNullValues() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        Config newConfig = new Config(config.getName(), null, null);

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(newConfig)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.name", is(newConfig.getName())))
                .andExpect(jsonPath("$.data.value", is("")))
                .andExpect(jsonPath("$.data.description", is(nullValue())));

        // Make sure the config is updated in the database
        Config updatedConfig = configRepository.findByName(config.getName());
        Assertions.assertEquals("", updatedConfig.getValue());
        Assertions.assertEquals(newConfig.getDescription(), updatedConfig.getDescription());
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
    public void testUpdateConfigMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
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
    public void testUpdateConfigMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
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
    public void testUpdateConfigNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/configservice/configs")
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
