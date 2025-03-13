package food.component.get;

import food.entity.TrainFood;
import food.repository.TrainFoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetTrainFoodTripTest {

    @Autowired
    private TrainFoodRepository trainFoodRepository;

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
        trainFoodRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a tripId that matches the ID of a TrainFood in the database
     * <li><b>Parameters:</b></li> A tripId that matches the tripId of a TrainFood in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all TrainFood that match the tripId
     * @throws Exception
     */
    @Test
    public void testGetTrainFoodTripElementInDatabase() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();
        TrainFood trainFood = new TrainFood();
        trainFood.setId(uuid);
        trainFood.setTripId(uuid.toString());
        trainFoodRepository.save(trainFood);

        // Act
        mockMvc.perform(get("/api/v1/foodmapservice/trainfoods/{tripId}", trainFood.getTripId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(uuid.toString())))
                .andExpect(jsonPath("$.data[0].tripId", is(uuid.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all TrainFoods that match the tripId. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTrainFoodTripMultipleId() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();
        TrainFood trainFood = new TrainFood();
        trainFood.setId(uuid);
        trainFood.setTripId(uuid.toString());
        trainFoodRepository.save(trainFood);

        // Act
        mockMvc.perform(get("/api/v1/foodmapservice/trainfoods/{tripId}", trainFood.getTripId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(uuid.toString())))
                .andExpect(jsonPath("$.data[0].tripId", is(uuid.toString())));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a tripId that does not match the ID of any TrainFoods in the database.
     * <li><b>Parameters:</b></li> a random tripId that does not exist as TrainFood in the database
     * <li><b>Expected result:</b></li> status 0, msg "No content", empty list of TrainFood
     * <li><b>Related Issue:</b></li> <b>F4a:</b> The return value for {@code trainFoodRepository.findByTripId(tripId)}
     * should check if the list is empty. Currently, it only checks if the list is not null, which will always result in a successful response.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetTrainFoodTripNotExists() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/foodmapservice/trainfoods/{tripId}", UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No content")))
                .andExpect(jsonPath("$.data").isEmpty());
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
    public void testGetTrainFoodTripMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/foodmapservice/trainfoods/{tripId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetTrainFoodTripMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/foodmapservice/trainfoods/{tripId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }
}
