package security.component.delete;

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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteSecurityConfigTest {

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

    @BeforeEach
    void beforeEach() {
        securityRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id given in paths
     * <li><b>Parameters:</b></li> an element that matches the id given in paths
     * <li><b>Expected result:</b></li> status 1, msg "Success", the provided id. The config is no longer in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteSecurityConfigMatchingId() throws Exception {
        // Arrange
        SecurityConfig storedConfig = populateDatabase();
        assertEquals(1L, securityRepository.count());

        String id = storedConfig.getId().toString();

        // Act
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(id)));

        // Make sure that the SecurityConfig was deleted from the database
        assertEquals(0L, securityRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Success", the provided id. Only the first id in paths is used to delete the config.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteSecurityConfigMultipleId() throws Exception {
        // Arrange
        SecurityConfig storedConfig = populateDatabase();
        assertEquals(1L, securityRepository.count());

        String id = storedConfig.getId().toString();

        // Act
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(id)));

        // Make sure that the SecurityConfig was deleted from the database
        assertEquals(0L, securityRepository.count());
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteSecurityConfigMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
    void testDeleteSecurityConfigMissingObject() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id given
     * in paths
     * <li><b>Parameters:</b></li> some random id that does not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the provided id. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>D1:</b> repository.delete() is performed before the check whether the object
     * exists, always resulting in a successful deletion.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteSecurityConfigNoMatchingId() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, securityRepository.count());

        UUID id = UUID.randomUUID();

        // Act
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", id.toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(id.toString())));

        // Make sure that the database was unchanged
        assertEquals(1L, securityRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give an id, which is not the correct format for an id.
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27b:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteSecurityConfigInvalidUUIDFormat() throws Exception {
        // Arrange
        String uuid = "Does not follow UUID format";

        // Act
        mockMvc.perform(delete("/api/v1/securityservice/securityConfigs/{id}", uuid))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private SecurityConfig populateDatabase() {
        SecurityConfig config = new SecurityConfig();
        config.setId(UUID.randomUUID());
        config.setName(UUID.randomUUID().toString());
        return securityRepository.save(config);
    }
}
