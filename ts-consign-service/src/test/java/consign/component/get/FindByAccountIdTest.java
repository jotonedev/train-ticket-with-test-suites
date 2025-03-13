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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindByAccountIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an existing accountId as the URL parameter
     * <li><b>Parameters:</b></li> an accountId that exists in the database as a ConsignRecord
     * <li><b>Expected result:</b></li> status 1, msg "Find consign by account id success", the list of ConsignRecords that match the accountId
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByConsigneeCorrectObject() throws Exception {
        // Arrange
        ConsignRecord record = createSampleConsignRecord();
        consignRepository.save(record);
        List<ConsignRecord> records = new ArrayList<>();
        records.add(record);

        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/account/{id}", record.getAccountId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find consign by account id success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(record.getId().toString())))
                .andExpect(jsonPath("$.data[0].accountId", is(record.getAccountId().toString())))
                .andExpect(jsonPath("$.data[0].orderId", is(record.getOrderId().toString())))
                .andExpect(jsonPath("$.data[0].consignee", is(record.getConsignee())))
                .andExpect(jsonPath("$.data[0].phone", is(record.getPhone())))
                .andExpect(jsonPath("$.data[0].price", is(record.getPrice())))
                .andExpect(jsonPath("$.data[0].weight", is(record.getWeight())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we provide an accountId that does not exist in the database
     * <li><b>Parameters:</b></li> a random accountId that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "No Content according to consignee", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByConsigneeZeroObjects() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/account/{id}", UUID.randomUUID())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Arrange
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content according to accountId")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one accountId as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the accountId given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Find consign by account id success", the list of ConsignRecords that match the accountId.
     * Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByConsigneeMultipleId() throws Exception {
        // Arrange
        ConsignRecord record = createSampleConsignRecord();
        consignRepository.save(record);
        List<ConsignRecord> records = new ArrayList<>();
        records.add(record);

        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/account/{id}", record.getAccountId(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find consign by account id success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(record.getId().toString())))
                .andExpect(jsonPath("$.data[0].accountId", is(record.getAccountId().toString())))
                .andExpect(jsonPath("$.data[0].orderId", is(record.getOrderId().toString())))
                .andExpect(jsonPath("$.data[0].consignee", is(record.getConsignee())))
                .andExpect(jsonPath("$.data[0].phone", is(record.getPhone())))
                .andExpect(jsonPath("$.data[0].price", is(record.getPrice())))
                .andExpect(jsonPath("$.data[0].weight", is(record.getWeight())));
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
    public void testFindByConsigneeMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/consignservice/consigns/account/{id}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testFindByConsigneeMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/consignservice/consigns/account/{id}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private ConsignRecord createSampleConsignRecord() {
        return new ConsignRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, 3.0);
    }
}
