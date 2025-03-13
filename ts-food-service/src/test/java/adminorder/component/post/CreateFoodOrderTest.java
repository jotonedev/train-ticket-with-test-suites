package adminorder.component.post;

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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FoodApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateFoodOrderTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to save a FoodStore that does not exist in the database with
     * a FoodType of 2
     * <li><b>Parameters:</b></li> A FoodStore with an ID that does not exist in the database, and a FoodType of 2
     * <li><b>Expected result:</b></li> status 1, msg "Success", the created FoodStore. The database contains the created Station.
     * Because of the new FoodType, stationName and storeName are also assigned to the values of the provided FoodOrder object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateFoodOrderCorrectObjectFoodType2() throws Exception {
        // Arrange
        FoodOrder foodOrder = createSampleFoodOder();
        String jsonRequest = new ObjectMapper().writeValueAsString(foodOrder);

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                // ID should be different, as a new id is generated
                .andExpect(jsonPath("$.data.id", not(foodOrder.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(foodOrder.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(foodOrder.getFoodType())))
                .andExpect(jsonPath("$.data.stationName", is(foodOrder.getStationName())))
                .andExpect(jsonPath("$.data.storeName", is(foodOrder.getStoreName())))
                .andExpect(jsonPath("$.data.foodName", is(foodOrder.getFoodName())))
                .andExpect(jsonPath("$.data.price", is(foodOrder.getPrice())));

        // Make sure the food order is stored
        assertEquals(1, foodOrderRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to save a FoodStore that does not exist in the database with
     * a FoodType that is not equal to 2
     * <li><b>Parameters:</b></li> A FoodStore with an ID that does not exist in the database, and a FoodType that is not 2
     * <li><b>Expected result:</b></li> status 1, msg "Success", the created FoodStore. The database contains the created Station.
     * Because of the new FoodType, stationName and storeName are not assigned to the values of the provided FoodOrder object, and remain null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateFoodOrderCorrectObjectFoodTypeNot2() throws Exception {
        // Arrange
        FoodOrder foodOrder = createSampleFoodOder();
        foodOrder.setFoodType(3);
        String jsonRequest = new ObjectMapper().writeValueAsString(foodOrder);

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                // ID should be different, as a new id is generated
                .andExpect(jsonPath("$.data.id", not(foodOrder.getId().toString())))
                .andExpect(jsonPath("$.data.orderId", is(foodOrder.getOrderId().toString())))
                .andExpect(jsonPath("$.data.foodType", is(foodOrder.getFoodType())))
                .andExpect(jsonPath("$.data.foodName", is(foodOrder.getFoodName())))
                .andExpect(jsonPath("$.data.price", is(foodOrder.getPrice())))
                // StationName and storeName should be null
                .andExpect(jsonPath("$.data.stationName", is(nullValue())))
                .andExpect(jsonPath("$.data.storeName", is(nullValue())));

        // Make sure the food order is stored
        assertEquals(1, foodOrderRepository.count());
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
    public void testCreateFoodOrderMultipleObjects() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        FoodOrder[] orders = {fo, fo};
        String jsonRequest = new ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());

    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a FoodOrder with an id that already exists in the database
     * <li><b>Parameters:</b></li> a FoodOrder with an id that already exists in the database
     * <li><b>Expected result:</b></li> status 0, msg "Order Id Has Existed", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateFoodOrderDuplicateObject() throws Exception {
        // Arrange
        FoodOrder fo = createSampleFoodOder();
        foodOrderRepository.save(fo);
        String jsonRequest = new ObjectMapper().writeValueAsString(fo);

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Id Has Existed.")))
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
    public void testCreateFoodOrderMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
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
    public void testCreateFoodOrderMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/foodservice/orders")
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
    public void testCreateFoodOrderNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/foodservice/orders")
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
