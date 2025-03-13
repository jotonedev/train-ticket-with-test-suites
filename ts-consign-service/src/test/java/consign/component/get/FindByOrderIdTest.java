package consign.component.get;

import consign.entity.ConsignRecord;
import consign.repository.ConsignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
public class FindByOrderIdTest {

    @Autowired
    private ConsignRepository consignRepository;

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
        consignRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an existing ID as the URL parameter
     * <li><b>Parameters:</b></li> an existing ID
     * <li><b>Expected result:</b></li> status 1, msg "Find consign by order id success", the found ConsignRecord.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdCorrectObject() throws Exception {
        // Arrange
        ConsignRecord record = createSampleConsignRecord();
        consignRepository.save(record);

        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/order/{id}", record.getOrderId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find consign by order id success")))
                .andExpect(jsonPath("$.data.id", is(record.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(record.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(record.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(record.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(record.getPhone())))
                .andExpect(jsonPath("$.data.price", is(record.getPrice())))
                .andExpect(jsonPath("$.data.weight", is(record.getWeight())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass an ID that does not match the ID of a ConsignRecord in the database
     * <li><b>Parameters:</b></li> some random Id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content according to order id", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdNotExists() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/order/{id}", UUID.randomUUID())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content according to order id")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Find consign by order id success", the found ConsignRecord. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdMultipleId() throws Exception {
        // Arrange
        ConsignRecord record = createSampleConsignRecord();
        consignRepository.save(record);

        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/order/{id}", record.getOrderId(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find consign by order id success")))
                .andExpect(jsonPath("$.data.id", is(record.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(record.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(record.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(record.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(record.getPhone())))
                .andExpect(jsonPath("$.data.price", is(record.getPrice())))
                .andExpect(jsonPath("$.data.weight", is(record.getWeight())));
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
    public void testFindByOrderIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/consignservice/consigns/order/{id}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/order/{id}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private ConsignRecord createSampleConsignRecord() {
        return new ConsignRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, 3.0);
    }
}
