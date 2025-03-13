package assurance.component.delete;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteAssuranceByOrderIdTest {

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
    public void beforeEach() {
        assuranceRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete an Assurance that does exist in the database
     * <li><b>Parameters:</b></li> An orderId that matches the order attribute of an Assurance in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success with Order Id", no data. The database no longer contains the Deleted Assurance.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteAssuranceByOrderIdCorrectObject() throws Exception {
        // Arrange
        Assurance expectedAssurance = configureAssurance();
        assertEquals(1L, assuranceRepository.count());

        UUID expectedOrderId = expectedAssurance.getOrderId();

        // Act
        mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", expectedOrderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success with Order Id")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the assurance was deleted from the database
        assertEquals(0L, assuranceRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one assuranceId as the URL parameter
     * <li><b>Parameters:</b></li> an orderId that matches the order of an assurance and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success with Order Id", no data. The database no longer contains the Deleted Assurance.
     * Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteAssuranceByOrderIdMultipleId() throws Exception {
        // Arrange
        Assurance expectedAssurance = configureAssurance();
        assertEquals(1L, assuranceRepository.count());

        UUID expectedOrderId = expectedAssurance.getOrderId();

        // Act
        mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", expectedOrderId, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success with Order Id")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the assurance was deleted from the database
        assertEquals(0L, assuranceRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to delete an assurance that does not exist in the database
     * <li><b>Parameters:</b></li> Some random orderId that does not match any assurances in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success with Order Id", no data. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>D1:</b> repository.delete() is performed before the check whether the object
     * exists, always resulting in a successful deletion.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteAssuranceByOrderIdNotExists() throws Exception {
        // Arrange
        UUID expectedOrderId = UUID.randomUUID();

        // Act
        mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", expectedOrderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success with Order Id")))
                .andExpect(jsonPath("$.data", nullValue()));

        assertEquals(0L, assuranceRepository.count());
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
    public void testDeleteAssuranceByOrderIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteAssuranceByOrderIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
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
    public void FAILING_testDeleteAssuranceByOrderIdInvalidUUIDFormat() throws Exception {
        // Arrange
        String uuid = "Does not follow UUID format";

        // Act
        mockMvc.perform(delete("/api/v1/assuranceservice/assurances/orderid/{orderId}", uuid))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Assurance configureAssurance() {
        Assurance assurance = new Assurance();
        assurance.setId(UUID.randomUUID());
        assurance.setOrderId(UUID.randomUUID());
        assurance.setType(AssuranceType.TRAFFIC_ACCIDENT);
        return assuranceRepository.save(assurance);
    }
}
