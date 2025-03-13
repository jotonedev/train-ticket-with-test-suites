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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindAssuranceByOrderIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we we provide an existing ID as the URL parameter
     * <li><b>Parameters:</b></li> an existing orderId that matches the order of an Assurance in the database
     * <li><b>Expected result:</b></li> status 1, msg "Find Assurace Success", the found Assurance object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindAssuranceByOrderIdCorrectObject() throws Exception {
        // Arrange
        Assurance assurance = configureAssurance(UUID.randomUUID(), UUID.randomUUID());
        assuranceRepository.save(assurance);

        // Act
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", assurance.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find Assurace Success")))
                .andExpect(jsonPath("$.data.id", is(assurance.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(assurance.getOrderId().toString())))
                .andExpect(jsonPath("$.data.type", is("TRAFFIC_ACCIDENT")));
    }
    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an id that matches an element in the database and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Find Assurace Success", the found Assurance. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindAssuranceByOrderIdMultipleId() throws Exception {
        // Arrange
        Assurance assurance = configureAssurance(UUID.randomUUID(), UUID.randomUUID());
        assuranceRepository.save(assurance);

        // Act
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", assurance.getOrderId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find Assurace Success")))
                .andExpect(jsonPath("$.data.id", is(assurance.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(assurance.getOrderId().toString())))
                .andExpect(jsonPath("$.data.type", is("TRAFFIC_ACCIDENT")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass an ID that does not match the ID of an Assurance in the database
     * <li><b>Parameters:</b></li> some random Id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Conotent by this id", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindAssuranceByOrderIdNotExists() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content by this orderId")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    public void testFindAssuranceByOrderIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/assuranceservice/assurance/orderid/{orderId}"));
        });
    }


    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testFindAssuranceByOrderIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurance/orderid/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
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
     * <ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testFindAssuranceByOrderIdInvalidUUIDFormat() throws Exception {
        // Arrange
        String uuid = "Does not follow UUID format";

        // Act
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/assuranceservice/assurance/orderid/{orderId}", uuid)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Assurance configureAssurance(UUID id, UUID orderId) {
        return new Assurance(id, orderId, AssuranceType.TRAFFIC_ACCIDENT);
    }
}
