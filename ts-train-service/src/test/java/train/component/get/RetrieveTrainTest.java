package train.component.get;

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
import train.entity.TrainType;
import train.repository.TrainTypeRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class RetrieveTrainTest {

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
     * <li><b>Tests:</b></li> That the endpoint successfully retrieves a trainType with the given ID.
     * <li><b>Parameters:</b></li> An ID for a trainType that exists in the database.
     * <li><b>Expected result:</b></li> status 1, msg "success", the trainType with the corresponding ID is returned.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveTrainTypeExistingId() throws Exception {
        // Arrange
        TrainType storedTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/trainservice/trains/{id}", storedTrainType.getId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("success")))
                .andExpect(jsonPath("$.data.id", is(storedTrainType.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> A trainType with a random id is stored in the database and a random id is provided
     * <li><b>Expected result:</b></li> status 1, msg "success", the provided trainType is returned. Only the first id is used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveTrainTypeMultipleId() throws Exception {
        // Arrange
        TrainType storedTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        // Act
        mockMvc.perform(get("/api/v1/trainservice/trains/{id}", storedTrainType.getId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("success")))
                .andExpect(jsonPath("$.data.id", is(storedTrainType.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when a trainType with the given id does not exist
     * <li><b>Parameters:</b></li> A random id is provided
     * <li><b>Expected result:</b></li> status 0, msg "here is no TrainType with the trainType id: " + id
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRetrieveTrainTypeMissingId() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/trainservice/trains/{id}", id))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("here is no TrainType with the trainType id: " + id)));
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
    public void testRetrieveTrainTypeNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/trainservice/trains/{id}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testRetrieveTrainTypeMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/trainservice/trains/{id}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private TrainType populateDatabase() {
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        return trainTypeRepository.save(trainType);
    }
}
