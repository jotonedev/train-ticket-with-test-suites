package adminorder.component.put;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FoodApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateFoodOrderTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a FoodStore that does exist in the database with
     * a FoodType of 1
     * <li><b>Parameters:</b></li> A FoodStore with an ID that matches the ID of a FoodStore in the database, and a FoodType of 1
     * <li><b>Expected result:</b></li> status 1, msg "Success", the updated FoodStore. The database contains the updated Station.
     * Because of the new FoodType, stationName and storeName are also updated
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateFoodOrderCorrectObjectFoodType1() throws Exception {
        // Arrange
        FoodOrder foodOrder = createSampleFoodOder();
        foodOrderRepository.save(foodOrder);

        FoodOrder updatedFoodOrder = new FoodOrder();
        updatedFoodOrder.setId(foodOrder.getId());
        updatedFoodOrder.setOrderId(foodOrder.getOrderId());
        updatedFoodOrder.setFoodName("newFoodName");
        updatedFoodOrder.setFoodType(1);
        updatedFoodOrder.setPrice(4.0);
        updatedFoodOrder.setStationName("newStationName");
        updatedFoodOrder.setStoreName("newStoreName");

        String jsonRequest = new ObjectMapper().writeValueAsString(updatedFoodOrder);

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(foodOrder.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(foodOrder.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(updatedFoodOrder.getFoodType())))
                .andExpect(jsonPath("$.data.stationName", is(updatedFoodOrder.getStationName())))
                .andExpect(jsonPath("$.data.storeName", is(updatedFoodOrder.getStoreName())))
                .andExpect(jsonPath("$.data.foodName", is(updatedFoodOrder.getFoodName())))
                .andExpect(jsonPath("$.data.price", is(updatedFoodOrder.getPrice())));

        // Make sure that the Station was updated in the database
        FoodOrder storedFoodOrder = foodOrderRepository.findById(foodOrder.getId());
        assertEquals(updatedFoodOrder, storedFoodOrder);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a FoodStore that does exist in the database with
     * a FoodType that is not 1
     * <li><b>Parameters:</b></li> A FoodStore with an ID that matches the ID of a FoodStore in the database, and a FoodType that is not 1
     * <li><b>Expected result:</b></li> status 1, msg "Success", the updated FoodStore. The database contains the updated Station.
     * Because of the new FoodType, stationName and storeName are not updated
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateFoodOrderCorrectObjectFoodTypeNot1() throws Exception {
        // Arrange
        FoodOrder foodOrder = createSampleFoodOder();
        foodOrderRepository.save(foodOrder);

        FoodOrder updatedFoodOrder = new FoodOrder();
        updatedFoodOrder.setId(foodOrder.getId());
        updatedFoodOrder.setOrderId(foodOrder.getOrderId());
        updatedFoodOrder.setFoodName("newFoodName");
        updatedFoodOrder.setFoodType(3);
        updatedFoodOrder.setPrice(4.0);
        updatedFoodOrder.setStationName("newStationName");
        updatedFoodOrder.setStoreName("newStoreName");

        String jsonRequest = new ObjectMapper().writeValueAsString(updatedFoodOrder);

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(foodOrder.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(foodOrder.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(updatedFoodOrder.getFoodType())))
                .andExpect(jsonPath("$.data.foodName", is(updatedFoodOrder.getFoodName())))
                .andExpect(jsonPath("$.data.price", is(updatedFoodOrder.getPrice())))
                // stationName and storeName should not be updated
                .andExpect(jsonPath("$.data.stationName", is(foodOrder.getStationName())))
                .andExpect(jsonPath("$.data.storeName", is(foodOrder.getStoreName())));

        // Make sure that the Station was updated in the database
        FoodOrder storedFoodOrder = foodOrderRepository.findById(foodOrder.getId());
        assertEquals(updatedFoodOrder.getFoodType(), storedFoodOrder.getFoodType());
        assertEquals(updatedFoodOrder.getFoodName(), storedFoodOrder.getFoodName());
        assertEquals(updatedFoodOrder.getPrice(), storedFoodOrder.getPrice());
        // stationName and storeName should not be updated
        assertEquals(foodOrder.getStationName(), storedFoodOrder.getStationName());
        assertEquals(foodOrder.getStoreName(), storedFoodOrder.getStoreName());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two FoodOrder objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateFoodOrderMultipleObjects() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        FoodOrder[] orders = {fo, fo};
        String jsonRequest = new ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());

    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a FoodOrder that does not already exists in the database
     * <li><b>Parameters:</b></li> a FoodOrder with a random id
     * <li><b>Expected result:</b></li> status 0, msg "Order Id Is Non-Existent.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateFoodOrderNotExist() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        String jsonRequest = new ObjectMapper().writeValueAsString(fo);

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Id Is Non-Existent.")))
                .andExpect(jsonPath("$.data", is(nullValue())));
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
    public void testUpdateFoodOrderMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
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
    public void testUpdateFoodOrderMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
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
    public void testUpdateFoodOrderNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
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
