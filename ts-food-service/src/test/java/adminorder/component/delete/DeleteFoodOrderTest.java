package adminorder.component.delete;

import foodsearch.FoodApplication;
import foodsearch.entity.FoodOrder;
import foodsearch.repository.FoodOrderRepository;
import org.junit.jupiter.api.Assertions;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FoodApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteFoodOrderTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a FoodOrder that does exist in the database
     * <li><b>Parameters:</b></li> An an ID that matches the ID of a FoodOrder in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data. The database no longer contains the Deleted FoodOrder.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteFoodOrderCorrectObject() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        foodOrderRepository.save(fo);
        Assertions.assertEquals(1, foodOrderRepository.count());

        // Act
        mockMvc.perform(delete("/api/v1/foodservice/orders/{orderId}", fo.getOrderId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the food order is deleted
        Assertions.assertEquals(0, foodOrderRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a FoodOrder that does not exist in the database
     * <li><b>Parameters:</b></li> Some random id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Order Id Is Non-Existent.", no data. The database is still empty.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteFoodOrderNotExist() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();

        // Act
        mockMvc.perform(delete("/api/v1/foodservice/orders/{orderId}", fo.getOrderId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Id Is Non-Existent.")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the database is still empty
        Assertions.assertEquals(0, foodOrderRepository.count());
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
    public void testDeleteFoodOrderNonExistingId()  {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/foodservice/orders/{orderId}"));
        });
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give an id, which is not the correct format for an id.
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27d:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteFoodOrderIncorrectFormatId() throws Exception {
        // Arrange
        String uuid = "not a correct format id";

        // Act
        mockMvc.perform(delete("/api/v1/foodservice/orders/{orderId}", uuid)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private FoodOrder createSampleFoodOder() {
        return new FoodOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                "station_name",
                "store_name",
                "food_name",
                3.0);
    }
}
