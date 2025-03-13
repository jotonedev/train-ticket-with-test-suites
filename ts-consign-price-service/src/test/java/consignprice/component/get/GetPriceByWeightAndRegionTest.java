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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Random;
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
public class GetPriceByWeightAndRegionTest {

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
     * <li><b>Tests:</b></li> that the endpoint behaves when there is a ConsignPrice in the database with index 0
     * <li><b>Parameters:</b></li> A weight smaller than the initial weight of the consignPrice and a boolean value of true to represent that the consignee is within the region
     * <li><b>Expected result:</b></li> status 1, msg "Success", 10.0. The price is set to the initial price of the consignPrice object
     */
    @Test
    public void testGetPriceByWeightAndRegionWeightSmaller() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();
        consignPriceConfigRepository.save(consignPrice);

        double weight = 2.0; // weight is <= than initial weight
        String isWithinRegion = "true";

        // price = initialPrice, since weight <= initial weight and is within region
        double expectedPrice = consignPrice.getInitialPrice();

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}", weight, isWithinRegion)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(expectedPrice)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that the endpoint behaves when there is a ConsignPrice in the database with index 0
     * <li><b>Parameters:</b></li> A weight greater than the initial weight of the consignPrice and a boolean value of true to represent that the consignee is within the region
     * <li><b>Expected result:</b></li> status 1, msg "Success", 20.0. The price is calculated as initialPrice + extraWeight * withinPrice
     */
    @Test
    public void testGetPriceByWeightAndRegionWeightBiggerWithinRegion() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();
        consignPriceConfigRepository.save(consignPrice);

        double weight = 10.0; // weight is > than initial weight
        String isWithinRegion = "true";

        // price = initialPrice + extraWeight * withinPrice, since weight > initial weight and is within region
        double expectedPrice = 10.0 + 5.0 * 2.0;

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}", weight, isWithinRegion)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(expectedPrice)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that the endpoint behaves when there is a ConsignPrice in the database with index 0
     * <li><b>Parameters:</b></li> A weight smaller than the initial weight of the consignPrice and a boolean value of false to represent that the consignee is not within the region
     * <li><b>Expected result:</b></li> status 1, msg "Success", 25.0. The price is calculated as initialPrice + extraWeight * beyondPrice
     */
    @Test
    public void testGetPriceByWeightAndRegionWeightBiggerNotWithinRegion() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();
        consignPriceConfigRepository.save(consignPrice);

        double weight = 10.0; // weight is > than initial weight
        String isWithinRegion = "false";

        // price = initialPrice + extraWeight * beyondPrice, since weight > initial weight and is within region
        double expectedPrice = 10.0 + 5.0 * 3.0;

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}", weight, isWithinRegion)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(expectedPrice)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more variables in the URL than it expects
     * <li><b>Parameters:</b></li> Some valid weight and isWithinRegion values and some random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", 20.0. The price is calculated as initialPrice + extraWeight * withinPrice. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetPriceByWeightAndRegionMultipleId() throws Exception {
        // Arrange
        ConsignPrice consignPrice = createSampleConsignPrice();
        consignPriceConfigRepository.save(consignPrice);

        double weight = 10.0; // weight is > than initial weight
        String isWithinRegion = "true";

        // price = initialPrice + extraWeight * withinPrice
        double expectedPrice = 10.0 + 5.0 * 2.0;

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}", weight, isWithinRegion, UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(expectedPrice)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no ConsignPrice object with index 0 in the database
     * <li><b>Parameters:</b></li> Some random weight and isWithinRegion values
     * <li><b>Expected result:</b></li> The service should handle the case where there is no ConsignPrice object with index 0 in the database.
     * The response should involve a status code of 0 with some appropriate message.
     * <li><b>Related Issue:</b></li> <b>F8b:</b> The service assumes that there will always be a ConsignPrice object with index 0 in the database.
     * This will lead to a NullPointerException that is unhandled if there is no ConsignPrice object with index 0 in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetPriceByWeightAndRegionNoConsignPriceAtIndex0() throws Exception {
        // Arrange
        Random random = new Random();

        double weight = random.nextDouble();
        boolean isWithinRegion = random.nextBoolean();

        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}", weight, isWithinRegion)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
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
    public void testGetPriceByWeightAndRegionMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetPriceByWeightAndRegionMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/consignpriceservice/consignprice/{weight}/{isWithinRegion}",UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
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
