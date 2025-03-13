package assurance.component.get;

import assurance.entity.Assurance;
import assurance.entity.AssuranceType;
import assurance.repository.AssuranceRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllAssurancesTest {

    @Autowired
    private AssuranceRepository assuranceRepository;

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
        assuranceRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that all stored Assurances are returned on endpoint call
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all PlainAssurances
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllAssurancesElementInDatabase() throws Exception {
        // Arrange
        for (int i = 0; i < 1000; i++) {
            Assurance assurance = new Assurance();
            assurance.setId(UUID.randomUUID());
            assurance.setType(AssuranceType.TRAFFIC_ACCIDENT);
            assuranceRepository.save(assurance);
        }

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances"))
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
     * <li><b>Tests:</b></li> how the endpoint behaves when not all of the stored Assurances are fully defined
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The service should handle the case where the Assurance is not fully defined in some way.
     * Whether that response should involve a status of 0 or 1 is up to the implementation, but the HTTP status should be 200.
     * <li><b>Related Issue:</b></li> <b>F8a:</b> The service assumes that all values in the database are not-null values,
     * and uses them as such without performing null-checks. This will lead to a NullPointerException that is unhandled
     * if there are assurances in the database containing null-values.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetAllAssurancesElementInDatabaseNotFullyDefined() throws Exception {
        // Arrange
        for (int i = 0; i < 1000; i++) {
            Assurance assurance = new Assurance();
            assurance.setId(UUID.randomUUID());
            assuranceRepository.save(assurance);
        }

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances"))
                .andDo(print())
                // Assert
                // The service implementation should handle the case where the Assurance is not fully defined in some way.
                // Whether that response should involve a status of 0 or 1 is up to the implementation, but the HTTP status
                // should be 200.
                .andExpect(status().isOk());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No Content, Assurance is empty", empty list of PlainAssurances
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllAssurancesEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content, Assurance is empty")))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
