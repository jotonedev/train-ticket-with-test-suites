package config.component.get;

import config.entity.Config;
import config.repository.ConfigRepository;
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
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RetrieveConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to get a Config that does exist in the database
     * <li><b>Parameters:</b></li> A name that matches the name of a Contact in the database
     * <li><b>Expected result:</b></li> status 1, msg "Find all  config success", the found config.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveConfigCorrectObject() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", config.getName())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(config.getValue())))
                .andExpect(jsonPath("$.data.description", is(config.getDescription())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one contactId as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", the found config. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveConfigCorrectObjectMultipleInputs() throws Exception {
        // Arrange
        Config config = prepareConfig();
        configRepository.save(config);

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", config.getName(), UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.name", is(config.getName())))
                .andExpect(jsonPath("$.data.value", is(config.getValue())))
                .andExpect(jsonPath("$.data.description", is(config.getDescription())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no config associated with the given config in the database
     * <li><b>Parameters:</b></li> some random config name that does not match the name of a config in the database
     * <li><b>Expected result:</b></li> status 0, msg "No content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveConfigNotExists() throws Exception {
        // Arrange
        String configName = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", configName)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No content")))
                .andExpect(jsonPath("$.data", is(nullValue())));
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
    public void testRetrieveConfigMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/configservice/configs/{configName}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testRetrieveConfigMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private Config prepareConfig() {
        return new Config("name", "0.5", "description");
    }
}
