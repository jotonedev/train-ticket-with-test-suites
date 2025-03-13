package train.component.post;

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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateTrainTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a TrainType that does not yet exist in the database
     * <li><b>Parameters:</b></li> A TrainType with a random ID
     * <li><b>Expected result:</b></li> status 1, msg "create success", the database contains the new TrainType
     * <li><b>Related Issue:</b></li> <b>F5a:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null == Optional.empty} to always be false. The service will therefore never
     * create a new Object, because it thinks that it already exists in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateValidObject() throws Exception {
        // Arrange
        TrainType newTrainType = new TrainType();
        newTrainType.setId(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newTrainType)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("create success")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the train type was saved to the database
        assertEquals(1L, trainTypeRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TrainType objects which should be created when sent individually
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateMultipleObjects() throws Exception {
        // Arrange
        TrainType newTrainType1 = new TrainType();
        newTrainType1.setId(UUID.randomUUID().toString());
        TrainType newTrainType2 = new TrainType();
        newTrainType2.setId(UUID.randomUUID().toString());

        TrainType[] trainTypes = {newTrainType1, newTrainType2};
        String jsonRequest = new ObjectMapper().writeValueAsString(trainTypes);

        // Act
        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a TrainType that already exists in the database
     * <li><b>Parameters:</b></li> a TrainType with an id that matches the id in the database
     * <li><b>Expected result:</b></li> status 0, msg "train type already exist", the provided TrainType. The database
     * remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateDuplicateObject() throws Exception {
        // Arrange
        TrainType existingTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        // Act
        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(existingTrainType)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("train type already exist")))
                .andExpect(jsonPath("$.data.id", is(existingTrainType.getId())));

        // Make sure that the train type was not saved to the database
        assertEquals(1L, trainTypeRepository.count());
        Optional<TrainType> optionalTrainType = trainTypeRepository.findById(existingTrainType.getId());
        assertTrue(optionalTrainType.isPresent());
        TrainType trainType = optionalTrainType.get();
        assertEquals(existingTrainType.getId(), trainType.getId());
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
    public void testCreateNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/trainservice/trains")
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
    public void testCreateMalformedRequestBody() throws Exception {
        // Arrange
        String badRequestBody = "{ \\\"someField\\\": \\\"some value\\\" }";

        // Act
        mockMvc.perform(post("/api/v1/trainservice/trains")
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
    public void testCreateEmptyRequestBody() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/trainservice/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString("")))
                .andDo(print())
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains("JSON parse error")));
    }

    private TrainType populateDatabase() {
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        return trainTypeRepository.save(trainType);
    }
}
