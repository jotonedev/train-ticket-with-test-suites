package food.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.fudan.common.util.Response;
import food.entity.FoodStore;
import food.repository.FoodStoreRepository;
import org.junit.jupiter.api.Assertions;
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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetFoodStoresByStationIdsTest {

    @Autowired
    private FoodStoreRepository foodStoreRepository;

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
        foodStoreRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a list containing stationIds that match a stationId in the database.
     * <li><b>Parameters:</b></li> A list of stationIds that match stationIds in the database.
     * <li><b>Expected result:</b></li> status 1, msg "Success", A list of FoodStores
     * @throws Exception
     */
    @Test
    public void testGetFoodStoresByStationIdsCorrectObject() throws Exception {
        // Arrange
        List<FoodStore> foodStoreList = new ArrayList<>();
        FoodStore fs1 = new FoodStore();
        fs1.setId(UUID.randomUUID());
        fs1.setStationId("1");
        foodStoreList.add(fs1);
        FoodStore fs2 = new FoodStore();
        fs2.setId(UUID.randomUUID());
        fs2.setStationId("2");
        foodStoreList.add(fs2);

        foodStoreRepository.saveAll(foodStoreList);

        List<String> stationIds = new ArrayList<>();
        stationIds.add("1");
        stationIds.add("2");
        String jsonRequest = new ObjectMapper().writeValueAsString(stationIds);

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].stationId", is("1")))
                .andExpect(jsonPath("$.data[1].stationId", is("2")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Station objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetFoodStoresByStationIdsMultipleObjects() throws Exception {
        // Arrange
        List<FoodStore> foodStoreList = new ArrayList<>();
        FoodStore fs1 = new FoodStore();
        fs1.setId(UUID.randomUUID());
        fs1.setStationId("1");
        foodStoreList.add(fs1);
        FoodStore fs2 = new FoodStore();
        fs2.setId(UUID.randomUUID());
        fs2.setStationId("2");
        foodStoreList.add(fs2);

        foodStoreRepository.saveAll(foodStoreList);

        List<ArrayList<String>> lists = new ArrayList<>();
        lists.add(new ArrayList<>(Arrays.asList("1", "2")));
        lists.add(new ArrayList<>(Arrays.asList("1", "2")));
        String jsonRequest = new ObjectMapper().writeValueAsString(lists);

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no stationId in the list of passed stationIds that matches
     * a stationId in the database
     * <li><b>Parameters:</b></li> a list containing a random stationId that does not exist in the database.
     * <li><b>Expected result:</b></li> status 0, msg "No content", no data
     * <li><b>Related Issue:</b></li> <b>F4a:</b> The return value for {@code foodStoreRepository.findByStationIdIn(stationIds)}
     * should check if the list is empty. Currently, it only checks if the list is not null, which will always result in a successful response.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetFoodStoresByStationIdsZeroObjects() throws Exception {
        // Arrange
        List<String> stationIds = new ArrayList<>();
        stationIds.add(UUID.randomUUID().toString());
        String jsonRequest = new ObjectMapper().writeValueAsString(stationIds);

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No content")))
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
    public void testGetFoodStoresByStationIdsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
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
    void testGetFoodStoresByStationIdsMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
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
    public void testGetFoodStoresByStationIdsNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when an empty list is passed as a request body
     * <li><b>Parameters:</b></li> an empty list
     * <li><b>Expected result:</b></li> status 0, msg "No content", no data
     * <li><b>Related Issue:</b></li> <b>F4a:</b> The return value for {@code foodStoreRepository.findByStationIdIn(stationIds)}
     * should check if the list is empty. Currently, it only checks if the list is not null, which will always result in a successful response.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetFoodStoresByStationIdsEmptyList() throws Exception {
        // Arrange
        List<String> stationIds = new ArrayList<>();
        String jsonRequest = new ObjectMapper().writeValueAsString(stationIds);

        // Act
        mockMvc.perform(post("/api/v1/foodmapservice/foodstores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No content")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }
}
