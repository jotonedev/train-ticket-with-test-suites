package consign.component.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import consign.entity.Consign;
import consign.entity.ConsignRecord;
import consign.repository.ConsignRepository;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateConsignTest {

    @Autowired
    private ConsignRepository consignRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

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
        mockServer = MockRestServiceServer.createServer(restTemplate);
        consignRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that
     * corresponds to an existing consign record in the repository, and the weight of the consign and consignRecord are the same.
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and the same weight as the consignRecord.
     * <li><b>Expected result:</b></li> status 1, msg "Update consign success", the updated consignRecord object. All values of the
     * consignRecord are set to the values of the provided Consign object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignCorrectObject() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();

        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        consignRepository.save(consignRecord);
        consign.setConsignee("updatedConsignee");
        consignRecord.setConsignee("updatedConsignee");

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Consign objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignMultipleObjects() throws Exception {
        // Arrange
        Consign consign1 = createSampleConsign();
        Consign consign2 = createSampleConsign();

        String jsonRequest = new ObjectMapper().writeValueAsString(Arrays.asList(consign1, consign2));

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignMissingObject() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }
    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that
     * corresponds to an existing consign record in the repository, but the weight of the consign and consignRecord are different.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and a different weight than the consignRecord.
     * <li><b>Expected result:</b></li> status 1, msg "Update consign success", the updated consignRecord object. All values of the
     * consignRecord are set to the values of the provided Consign object, the price is updated to the new price which is returned by
     * the ts-consign-price-service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignCorrectObjectNewWeight() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();
        ConsignRecord consignRecord = createSampleConsignRecord();
        consignRecord.setAccountId(consign.getAccountId());
        consignRecord.setId(consign.getId());
        consignRecord.setOrderId(consign.getOrderId());
        // consignRecord weight is different from consign weight
        consignRecord.setWeight(2.0);
        consignRepository.save(consignRecord);
        consignRecord.setWeight(1.0);
        consignRecord.setPrice(3.0);

        Response<Double> responseTrainFood = new Response<>(1, "Success", 25.0);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/"
                + consign.getWeight() + "/" + consign.isWithin()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update consign success")))
                .andExpect(jsonPath("$.data.id", is(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                .andExpect(jsonPath("$.data.price", is(25.0)))
                .andExpect(jsonPath("$.data.weight", is(consign.getWeight())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid consign object to the endpoint with an id that does
     * not correspond to an existing consign record in the repository.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-consign-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a Consign object with valid values for all attributes and an id that does not correspond
     * to an existing consign record.
     * <li><b>Expected result:</b></li> status 1, msg "You have consigned successfully! The price is " + price, the consignRecord object.
     * The update implementation of this service calls the method for creating a new consign record in the repository, and the price is
     * updated to the new price which is returned by the ts-consign-price-service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateConsignCorrectObjectIdNotExists() throws Exception {
        // Arrange
        Consign consign = createSampleConsign();

        Response<Double> responseTrainFood = new Response<>(1, "Success", 25.0);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-consign-price-service:16110/api/v1/consignpriceservice/consignprice/"
                + consign.getWeight() + "/" + consign.isWithin()).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        String jsonRequest = new ObjectMapper().writeValueAsString(consign);

        // Act
        mockMvc.perform(put("/api/v1/consignservice/consigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("You have consigned successfully! The price is 25.0")))
                // The consign record has been created with a new id that is different from the consign id
                .andExpect(jsonPath("$.data.id", not(consign.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(consign.getAccountId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(consign.getOrderId().toString())))
                .andExpect(jsonPath("$.data.consignee", is(consign.getConsignee())))
                .andExpect(jsonPath("$.data.phone", is(consign.getPhone())))
                .andExpect(jsonPath("$.data.price", is(25.0)))
                .andExpect(jsonPath("$.data.weight", is(consign.getWeight())));

        mockServer.verify();
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
