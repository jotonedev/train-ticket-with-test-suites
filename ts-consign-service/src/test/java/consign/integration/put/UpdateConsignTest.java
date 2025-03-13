package consign.integration.put;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import consign.entity.Consign;
import consign.entity.ConsignRecord;
import consign.repository.ConsignRepository;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UpdateConsignTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected ConsignRepository consignRepository;

    private final static Network network = Network.newNetwork();

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @Container
    public static final MongoDBContainer consignPriceServiceMongoDBContainer = new MongoDBContainer("mongo:latest")
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-mongo");

    @Container
    public static GenericContainer<?> consignPriceContainer = new GenericContainer<>(DockerImageName.parse("local/ts-consign-price-service:0.1"))
            .withImagePullPolicy(PullPolicy.defaultPolicy())
            .withExposedPorts(16110)
            .withNetwork(network)
            .withNetworkAliases("ts-consign-price-service")
            .dependsOn(consignPriceServiceMongoDBContainer);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("ts.consign.price.service.url", consignPriceContainer::getHost);
        registry.add("ts.consign.price.service.port", () -> consignPriceContainer.getMappedPort(16110));
    }

    @BeforeEach
    public void setUp() {
        consignRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Called by ts-consign-service:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that
     * corresponds to an existing consign record in the repository, and the weight of the consign and consignRecord are the same.
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and the same weight as the consignRecord.
     * <li><b>Expected result:</b></li> status 1, msg "Update consign success", the updated consignRecord object. All values of the
     * consignRecord are set to the values of the provided Consign object.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(1)
    public void testUpdateConsignCorrectObjectSameWeight() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setPrice(8.0);
        consignRepository.save(consignRecord);

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update consign success")))
                .andExpect(jsonPath("$.data.id", is(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                .andExpect(jsonPath("$.data.price", is(consignRecord.getPrice())))
                .andExpect(jsonPath("$.data.weight", is(consign.getWeight())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-consign-service:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that
     * corresponds to an existing consign record in the repository, but the weight of the consign and consignRecord are different.
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and a different weight than the consignRecord.
     * <li><b>Expected result:</b></li> status 1, msg "Update consign success", the updated consignRecord object. All values of the
     * consignRecord are set to the values of the provided Consign object, the price is updated to the new price which is returned by
     * the ts-consign-price-service.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testUpdateConsignCorrectObjectDifferentWeight() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(2.0);
        consignRepository.save(consignRecord);

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update consign success")))
                .andExpect(jsonPath("$.data.id", is(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                // 8.0 is the price returned by the consign-price-service
                .andExpect(jsonPath("$.data.price", is(8.0)))
                .andExpect(jsonPath("$.data.weight", is(consign.getWeight())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-consign-service:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that does
     * not correspond to an existing consign record in the repository.
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and an id that does not correspond
     * to an existing consign record.
     * <li><b>Expected result:</b></li> status 1, msg "You have consigned successfully! The price is " + price, the consignRecord object.
     * The update implementation of this service calls the method for creating a new consign record in the repository, and the price is
     * updated to the new price which is returned by the ts-consign-price-service.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(3)
    public void testUpdateConsignConsignIdNotExists() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setPrice(8.0);
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setAccountId(consign.getAccountId());

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("You have consigned successfully! The price is 8.0")))
                // The consign record has been created with a new id that is different from the consign id
                .andExpect(jsonPath("$.data.id", not(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                // 8.0 is the price returned by the consign-price-service
                .andExpect(jsonPath("$.data.price", is(8.0)))
                .andExpect(jsonPath("$.data.weight", is(consign.getWeight())));
    }

    /**
     * <ul>
     * <li><b>Called by ts-consign-service:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object with edge case values for the weight and price attribute.
     * <li><b>Parameters:</b></li> a Consign object with edge case values for the weight and price attribute.
     * <li><b>Expected result:</b></li> status 1, msg "Update consign success", the updated consignRecord object.
     * Edge case values are not checked in the implementation. Therefore, MAX or MIN values are accepted.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testUpdateConsignEdgeCaseWeightAndPrice() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        consign.setWeight(Double.MAX_VALUE);
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(Double.MAX_VALUE);
        consignRecord.setPrice(Double.MAX_VALUE);
        consignRepository.save(consignRecord);

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        String result = mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update consign success")))
                .andExpect(jsonPath("$.data.id", is(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                .andReturn().getResponse().getContentAsString();

        Response<ConsignRecord> response = JSONObject.parseObject(result, new TypeReference<Response<ConsignRecord>>() {});
        Assertions.assertEquals(Double.MAX_VALUE, response.getData().getWeight());
        Assertions.assertEquals(Double.MAX_VALUE, response.getData().getPrice());
    }

    /**
     * <ul>
     * <li><b>Called by ts-consign-service:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> where one or more external services are not available
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    @Order(5)
    public void testUpdateConsignServiceUnavailable() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRecord.setWeight(2.0);
        consignRepository.save(consignRecord);
        consignRecord.setWeight(1.0);
        consignRecord.setPrice(8.0);

        consignPriceContainer.stop();

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Consign createSampleConsign() {
        return new Consign(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, true);
    }


    private ConsignRecord createSampleConsignRecord() {
        return new ConsignRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "handle_date", "target_date",
                "place_from", "place_to", "consignee", "10001", 1.0, 3.0);
    }
}
