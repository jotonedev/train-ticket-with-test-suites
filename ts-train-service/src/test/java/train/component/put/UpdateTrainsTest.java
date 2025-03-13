package train.component.put;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import train.entity.TrainType;
import train.repository.TrainTypeRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateTrainsTest {

    @Autowired
    private TrainTypeRepository trainTypeRepository;

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
        trainTypeRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id given in requestBody
     * <li><b>Parameters:</b></li> a TrainType with an id that matches the id in the database
     * <li><b>Expected result:</b></li> status 1, msg "update success", a boolean representing the success of the update.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateTrainTypeCorrectObject() throws Exception {
        // Arrange
        TrainType storedTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        TrainType updatedTrainType = new TrainType();
        updatedTrainType.setId(storedTrainType.getId());
        updatedTrainType.setEconomyClass(1);
        updatedTrainType.setConfortClass(2);
        updatedTrainType.setAverageSpeed(3);

        // Act
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedTrainType)))
                .andDo(print())
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("update success")))
                .andExpect(jsonPath("$.data", is(true)));

        // Make sure that the train type was updated in the database
        assertEquals(1L, trainTypeRepository.count());
        Optional<TrainType> optionalTrainType = trainTypeRepository.findById(storedTrainType.getId());
        assertTrue(optionalTrainType.isPresent());
        TrainType trainType = optionalTrainType.get();
        assertEquals(updatedTrainType.getId(), trainType.getId());
        assertEquals(updatedTrainType.getEconomyClass(), trainType.getEconomyClass());
        assertEquals(updatedTrainType.getConfortClass(), trainType.getConfortClass());
        assertEquals(updatedTrainType.getAverageSpeed(), trainType.getAverageSpeed());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TrainType objects which should be updated when sent individually
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateTrainTypeMultipleObjects() throws Exception {
        // Arrange
        TrainType storedTrainType1 = populateDatabase();
        TrainType storedTrainType2 = populateDatabase();
        assertEquals(2L, trainTypeRepository.count());

        TrainType[] trainTypes = getTrainTypeArray(storedTrainType1, storedTrainType2);
        String jsonRequest = new ObjectMapper().writeValueAsString(trainTypes);

        // Act
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id given
     * in paths
     * <li><b>Parameters:</b></li> some random id that does not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the provided id. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>F5a:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null != Optional.empty} to always be true. The service will therefore try to update
     * and return a successful response every time.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testUpdateTrainTypeMissingObject() throws Exception {
        // Arrange
        TrainType newTrainType = new TrainType();
        newTrainType.setId(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newTrainType)))
                .andDo(print())
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("there is no trainType with the trainType id")))
                .andExpect(jsonPath("$.data", is(false)));
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
    public void testUpdateTrainTypeNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                .andDo(print())
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
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
    public void testUpdateTrainTypeMalformedRequestBody() throws Exception {
        // Arrange
        String badRequestBody = "{ \\\"someField\\\": \\\"some value\\\" }";

        // Act
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(badRequestBody)))
                .andDo(print())
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains("JSON parse error")));
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
    public void testUpdateTrainTypeEmptyRequestBody() throws Exception {
        // Act
        mockMvc.perform(put("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString("")))
                .andDo(print())
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains("JSON parse error")));
    }

    private TrainType[] getTrainTypeArray(TrainType storedTrainType1, TrainType storedTrainType2) {
        TrainType updatedTrainType1 = new TrainType();
        updatedTrainType1.setId(storedTrainType1.getId());
        updatedTrainType1.setEconomyClass(1);
        updatedTrainType1.setConfortClass(2);
        updatedTrainType1.setAverageSpeed(3);

        TrainType updatedTrainType2 = new TrainType();
        updatedTrainType2.setId(storedTrainType2.getId());
        updatedTrainType2.setEconomyClass(4);
        updatedTrainType2.setConfortClass(5);
        updatedTrainType2.setAverageSpeed(6);

        return new TrainType[]{updatedTrainType1, updatedTrainType2};
    }

    private TrainType populateDatabase() {
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        return trainTypeRepository.save(trainType);
    }
}
