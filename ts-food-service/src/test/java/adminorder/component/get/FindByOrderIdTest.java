package adminorder.component.get;

import foodsearch.FoodApplication;
import foodsearch.entity.FoodOrder;
import foodsearch.repository.FoodOrderRepository;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FoodApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindByOrderIdTest {

    @Autowired
    private FoodOrderRepository foodOrderRepository;

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
        foodOrderRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we we provide an existing ID as the URL parameter
     * <li><b>Parameters:</b></li> an existing orderId
     * <li><b>Expected result:</b></li> status 1, msg "Success.", the found FoodOrder object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdCorrectObject() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        foodOrderRepository.save(fo);

        // Act
        mockMvc.perform(get("/api/v1/foodservice/orders/{orderId}", fo.getOrderId().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data.id", is(fo.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(fo.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(fo.getFoodType())))
                .andExpect(jsonPath("$.data.stationName", is(fo.getStationName())))
                .andExpect(jsonPath("$.data.storeName", is(fo.getStoreName())))
                .andExpect(jsonPath("$.data.price", is(fo.getPrice())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass an ID that does not match the ID of a FoodOrder in the database
     * <li><b>Parameters:</b></li> some random Id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Order Id Is Non-Existent.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdNotExist() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();

        // Act
        mockMvc.perform(get("/api/v1/foodservice/orders/{orderId}", fo.getOrderId().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Id Is Non-Existent.")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an id that matches and element in the database and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success.", the found FoodOrder. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByOrderIdMultipleId() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        foodOrderRepository.save(fo);

        // Act
        mockMvc.perform(get("/api/v1/foodservice/orders/{orderId}", fo.getOrderId().toString(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data.id", is(fo.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(fo.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(fo.getFoodType())))
                .andExpect(jsonPath("$.data.stationName", is(fo.getStationName())))
                .andExpect(jsonPath("$.data.storeName", is(fo.getStoreName())))
                .andExpect(jsonPath("$.data.price", is(fo.getPrice())));
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
            mockMvc.perform(get("/api/v1/foodservice/orders/{orderId}"));
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
        mockMvc.perform(get("/api/v1/foodservice/orders/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private FoodOrder createSampleFoodOder() {
        return new FoodOrder(UUID.randomUUID(), UUID.randomUUID(), 2, "station_name", "store_name", "food_name", 3.0);
    }
}
