package config.component.delete;

import config.entity.Config;
import config.repository.ConfigRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a config that does exist in the database
     * <li><b>Parameters:</b></li> An config name that matches the name of a config in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the Deleted config. The database no longer contains that config.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteConfigCorrectObject() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", config.getName())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(config.getValue())))
                .andExpect(jsonPath("$.data.description", is(config.getDescription())));

        // Make sure the config is deleted
        Assertions.assertEquals(0, configRepository.findAll().size());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the deleted config. Only the first id in paths is used to delete the config.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteConfigCorrectObjectMultipleInputs() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", config.getName(), UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(config.getValue())))
                .andExpect(jsonPath("$.data.description", is(config.getDescription())));

        // Make sure the config is deleted
        Assertions.assertEquals(0, configRepository.findAll().size());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the config name given
     * in paths
     * <li><b>Parameters:</b></li> some random config name that does not match any element in the database
     * <li><b>Expected result:</b></li> status 0, msg "Config name doesn't exist", no data. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteConfigNotExists() throws Exception {
        // Arrange
        String configName = UUID.randomUUID().toString();

        // Act
        String result = mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", configName)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Config " + configName + " doesn't exist.")))
                .andExpect(jsonPath("$.data", is(nullValue())))
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteConfigMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", UUID.randomUUID() + "/" + UUID.randomUUID())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when nothing is passed to the endpoint
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> an IllegalArgumentException
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testDeleteConfigNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/configservice/configs/{configName}"));
        });
    }

    private Config prepareConfig() {
        return new Config("name", "0.5", "description");
    }
}
