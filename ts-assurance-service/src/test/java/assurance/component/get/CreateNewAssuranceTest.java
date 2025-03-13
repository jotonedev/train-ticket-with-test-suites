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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateNewAssuranceTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try adding an assurance already exists
     * <li><b>Parameters:</b></li> a typeIndex and orderId that already exists in the database
     * <li><b>Expected result:</b></li> status 0, msg "Fail.Assurance already exists", no data. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewAssuranceAlreadyExists() throws Exception {
        // Arrange
        Assurance expectedAssurance = populateDatabase();
        assertEquals(1L, assuranceRepository.count());

        UUID expectedOrderId = expectedAssurance.getOrderId();
        int expectedTypeIndex = expectedAssurance.getType().getIndex();

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", expectedTypeIndex, expectedOrderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Fail.Assurance already exists")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the assurance was not added to the database
        assertEquals(1L, assuranceRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try adding an assurance where the assurance type doesn't exist
     * <li><b>Parameters:</b></li> an orderId that doesn't yet exist in the database, and a typeIndex that doesn't exist
     * <li><b>Expected result:</b></li> status 0, msg "Fail.Assurance type doesn't exist", no data. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewAssuranceTypeNotExists() throws Exception {
        // Arrange
        UUID expectedOrderId = UUID.randomUUID();
        int expectedTypeIndex = 5;

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", expectedTypeIndex, expectedOrderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Fail.Assurance type doesn't exist")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the assurance was not added to the database
        assertEquals(0L, assuranceRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try adding an assurance with a new orderId and typeIndex that does exist
     * <li><b>Parameters:</b></li> an orderId that doesn't yet exist in the database, and a typeIndex that does exist
     * <li><b>Expected result:</b></li> status 1, msg "Success", the added assurance object. The database now contains the added assurance.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewAssuranceCorrectObject() throws Exception {
        // Arrange
        UUID expectedOrderId = UUID.randomUUID();
        int expectedTypeIndex = 1;

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", expectedTypeIndex, expectedOrderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.orderId", is(expectedOrderId.toString())))
                .andExpect(jsonPath("$.data.type", is("TRAFFIC_ACCIDENT")));

        // Make sure that the assurance was added to the database
        assertEquals(1L, assuranceRepository.count());
        assertNotNull(assuranceRepository.findByOrderId(expectedOrderId));
        assertEquals(expectedOrderId, assuranceRepository.findByOrderId(expectedOrderId).getOrderId());
        assertEquals(expectedTypeIndex, assuranceRepository.findByOrderId(expectedOrderId).getType().getIndex());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more variables in the URL than it expects
     * <li><b>Parameters:</b></li> an orderId that doesn't yet exist in the database, and a typeIndex that does exist, and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", the added assurance object. The database now contains the added assurance. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewAssuranceMultipleVariables() throws Exception {
        // Arrange
        UUID expectedOrderId = UUID.randomUUID();
        int expectedTypeIndex = 1;

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", expectedTypeIndex, expectedOrderId, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.orderId", is(expectedOrderId.toString())))
                .andExpect(jsonPath("$.data.type", is("TRAFFIC_ACCIDENT")));

        // Make sure that the assurance was added to the database
        assertEquals(1L, assuranceRepository.count());
        assertNotNull(assuranceRepository.findByOrderId(expectedOrderId));
        assertEquals(expectedOrderId, assuranceRepository.findByOrderId(expectedOrderId).getOrderId());
        assertEquals(expectedTypeIndex, assuranceRepository.findByOrderId(expectedOrderId).getType().getIndex());
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
    public void testCreateNewAssuranceMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testCreateNewAssuranceMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the id is not in the correct UUID format
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27a:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateNewAssuranceInvalidUUIDFormat() throws Exception {
        // Arrange
        String orderId = "Does not follow UUID format";
        int typeIndex = 1;

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/{typeIndex}/{orderId}", typeIndex, orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Assurance populateDatabase() {
        Assurance assurance = new Assurance();
        assurance.setId(UUID.randomUUID());
        assurance.setOrderId(UUID.randomUUID());
        assurance.setType(AssuranceType.TRAFFIC_ACCIDENT);
        return assuranceRepository.save(assurance);
    }
}
