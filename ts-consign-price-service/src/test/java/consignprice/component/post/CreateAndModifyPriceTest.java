package consignprice.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import consignprice.entity.ConsignPrice;
import consignprice.repository.ConsignPriceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateAndModifyPriceTest {

    @Autowired
    private ConsignPriceConfigRepository consignPriceConfigRepository;

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
        consignPriceConfigRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to create a new ConsignPrice object that does
     * not exist in the database.
     * <li><b>Parameters:</b></li> A valid ConsignPrice object with valid values for all attributes
     * <li><b>Expected result:</b></li> status 1, msg "Success", the created ConsignPrice object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifySuccessfulCreation() throws Exception {
        ConsignPrice consignPrice = createSampleConsignPrice();

        String jsonRequest = new ObjectMapper().writeValueAsString(consignPrice);

        mockMvc.perform(post("/api/v1/consignpriceservice/consignprice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(consignPrice.getId().toString())))
                .andExpect(jsonPath("$.data.index", is(consignPrice.getIndex())))
                .andExpect(jsonPath("$.data.initialPrice", is(consignPrice.getInitialPrice())))
                .andExpect(jsonPath("$.data.initialWeight", is(consignPrice.getInitialWeight())))
                .andExpect(jsonPath("$.data.withinPrice", is(consignPrice.getWithinPrice())))
                .andExpect(jsonPath("$.data.beyondPrice", is(consignPrice.getBeyondPrice())));

        // Make sure the object is actually saved in the database
        assertEquals(1, consignPriceConfigRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to create a new ConsignPrice object when there is already
     * one ConsignPrice object in the database.
     * <li><b>Parameters:</b></li> A valid ConsignPrice object with valid values for all attributes
     * <li><b>Expected result:</b></li> status 1, msg "Success", the ConsignPrice object. The ConsignPrice object is updated
     * to the new values in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifySuccessfulUpdate() throws Exception {
        consignPriceConfigRepository.save(createSampleConsignPrice());

        ConsignPrice updatedConsignPrice = createSampleConsignPrice();
        updatedConsignPrice.setBeyondPrice(4.0);
        updatedConsignPrice.setInitialPrice(20.0);
        updatedConsignPrice.setInitialWeight(10.0);
        updatedConsignPrice.setWithinPrice(3.0);

        String jsonRequest = new ObjectMapper().writeValueAsString(updatedConsignPrice);

        mockMvc.perform(post("/api/v1/consignpriceservice/consignprice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(updatedConsignPrice.getId().toString())))
                .andExpect(jsonPath("$.data.index", is(updatedConsignPrice.getIndex())))
                .andExpect(jsonPath("$.data.initialPrice", is(updatedConsignPrice.getInitialPrice())))
                .andExpect(jsonPath("$.data.initialWeight", is(updatedConsignPrice.getInitialWeight())))
                .andExpect(jsonPath("$.data.withinPrice", is(updatedConsignPrice.getWithinPrice())))
                .andExpect(jsonPath("$.data.beyondPrice", is(updatedConsignPrice.getBeyondPrice())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we attempt to create a new ConsignPrice object when there are already
     * two or more ConsignPrice objects in the database.
     * <li><b>Parameters:</b></li> A valid ConsignPrice object with valid values for all attributes
     * <li><b>Expected result:</b></li> status 1, msg "Success", the created ConsignPrice object.
     * <li><b>Related Issue:</b></li> <b>F7:</b> ConsignPrice objects are always stored in the repository with index 0.
     * Since the service implementation attempts to perform {@code repository.findByIndex(0}, which returns only one object,
     * the service will crash because there are multiple objects with index 0 in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateAndModifyMultipleConsignPriceInDatabase() throws Exception {
        consignPriceConfigRepository.save(createSampleConsignPrice());
        consignPriceConfigRepository.save(createSampleConsignPrice());

        ConsignPrice newConsignPrice = createSampleConsignPrice();
        String jsonRequest = new ObjectMapper().writeValueAsString(newConsignPrice);

        mockMvc.perform(post("/api/v1/consignpriceservice/consignprice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(newConsignPrice.getId().toString())))
                .andExpect(jsonPath("$.data.index", is(newConsignPrice.getIndex())))
                .andExpect(jsonPath("$.data.initialPrice", is(newConsignPrice.getInitialPrice())))
                .andExpect(jsonPath("$.data.initialWeight", is(newConsignPrice.getInitialWeight())))
                .andExpect(jsonPath("$.data.withinPrice", is(newConsignPrice.getWithinPrice())))
                .andExpect(jsonPath("$.data.beyondPrice", is(newConsignPrice.getBeyondPrice())));

        // Make sure the object is actually saved in the database
        assertEquals(3, consignPriceConfigRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two ConsignPrice objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyMultipleObjects() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();
        ConsignPrice[] array = {consignPrice, consignPrice};
        String jsonRequest = new ObjectMapper().writeValueAsString(array);

        // Act
        mockMvc.perform(post("/api/v1/consignpriceservice/consignprice")
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
    public void testCreateAndModifyMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testCreateAndModifyMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyNulLRequestBody() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private ConsignPrice createSampleConsignPrice() {
        ConsignPrice consignPrice = new ConsignPrice();
        consignPrice.setId(UUID.randomUUID());
        consignPrice.setIndex(0);
        consignPrice.setInitialPrice(10.0);
        consignPrice.setInitialWeight(5.0);
        consignPrice.setWithinPrice(2.0);
        consignPrice.setBeyondPrice(3.0);
        return consignPrice;
    }
}
