package consignprice.component.get;

import consignprice.entity.ConsignPrice;
import consignprice.repository.ConsignPriceConfigRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetPriceConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we have a ConsignPrice object in the database
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", the ConsignPrice object with index 0.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetPriceConfigObjectInDatabase() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();

        consignPriceConfigRepository.save(consignPrice);

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/config")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(consignPrice.getId().toString())))
                .andExpect(jsonPath("$.data.index", is(consignPrice.getIndex())))
                .andExpect(jsonPath("$.data.initialPrice", is(consignPrice.getInitialPrice())))
                .andExpect(jsonPath("$.data.initialWeight", is(consignPrice.getInitialWeight())))
                .andExpect(jsonPath("$.data.withinPrice", is(consignPrice.getWithinPrice())))
                .andExpect(jsonPath("$.data.beyondPrice", is(consignPrice.getBeyondPrice())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we have multiple ConsignPrice objects in the database
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", the ConsignPrice object with index 0.
     * <li><b>Related Issue:</b></li> <b>F7:</b> ConsignPrice objects are always stored in the repository with index 0.
     * Since the service implementation attempts to perform {@code repository.findByIndex(0}, which returns only one object,
     * the service will crash because there are multiple objects with index 0 in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetPriceConfigMultipleObjectsInDatabase() throws Exception {
        // Arrange
        consignPriceConfigRepository.save(createSampleConsignPrice());

        ConsignPrice consignPrice = createSampleConsignPrice();
        consignPriceConfigRepository.save(consignPrice);

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/config")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(consignPrice.getId().toString())))
                .andExpect(jsonPath("$.data.index", is(consignPrice.getIndex())))
                .andExpect(jsonPath("$.data.initialPrice", is(consignPrice.getInitialPrice())))
                .andExpect(jsonPath("$.data.initialWeight", is(consignPrice.getInitialWeight())))
                .andExpect(jsonPath("$.data.withinPrice", is(consignPrice.getWithinPrice())))
                .andExpect(jsonPath("$.data.beyondPrice", is(consignPrice.getBeyondPrice())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no ConsignPrice object with index 0 in the database
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The service should handle the case where there is no ConsignPrice object in the database.
     * The response should involve a status code of 0 with some appropriate message.
     * <li><b>Related Issue:</b></li> <b>F8b:</b> The service assumes that there will always be a ConsignPrice object in the database.
     * This will lead to a NullPointerException that is unhandled if there is no ConsignPrice object with index 0 in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetPriceConfigNoObjectInDatabase() throws Exception {
        // Arrange
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/config")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
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
