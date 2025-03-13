package security.component.get;

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
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindAllSecurityConfigTest {

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
     * <li><b>Tests:</b></li> that all stored securityConfigs are returned on endpoint call
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all securityConfigs
     * </ul>
     * @throws Exception
     */
    @Test
    void testFindAllSecurityConfigElementInDatabase() throws Exception {
        // Arrange
        for (int i = 0; i < 1000; i++) {
            SecurityConfig securityConfig = new SecurityConfig();
            securityConfig.setId(UUID.randomUUID());
            securityRepository.save(securityConfig);
        }

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1000)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No Content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindAllSecurityConfigEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
