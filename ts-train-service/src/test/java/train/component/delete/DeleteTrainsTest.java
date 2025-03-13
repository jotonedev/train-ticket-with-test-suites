package train.component.delete;

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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteTrainsTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains an element that matches the id given in paths
     * <li><b>Parameters:</b></li> an id that matches an id in the database
     * <li><b>Expected result:</b></li> status 1, msg "delete success", a boolean representing the success of the deletion.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTrainTypeMatchingId() throws Exception {
        // Arrange
        TrainType storedTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        // Act
        mockMvc.perform(delete("/api/v1/trainservice/trains/{id}", storedTrainType.getId()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("delete success")))
                .andExpect(jsonPath("$.data", is(true)));

        // Make sure that the train type was deleted from the database
        assertEquals(0L, trainTypeRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an id that matches an id in the database and a random id
     * <li><b>Expected result:</b></li> status 1, msg "delete success", a boolean representing the success of the deletion. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTrainTypeMultipleId() throws Exception {
        // Arrange
        TrainType storedTrainType = populateDatabase();
        assertEquals(1L, trainTypeRepository.count());

        // Act
        mockMvc.perform(delete("/api/v1/trainservice/trains/{id}", storedTrainType.getId(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("delete success")))
                .andExpect(jsonPath("$.data", is(true)));

        // Make sure that the train type was deleted from the database
        assertEquals(0L, trainTypeRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id given
     * in paths
     * <li><b>Parameters:</b></li> some random id that does not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the provided id. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>F5a:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null != Optional.empty} to always be true. The service will therefore try to delete
     * and return a successful response every time.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteTrainTypeNonExistingId() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/trainservice/trains/{id}", id))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("there is no train according to id")))
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
    public void testDeleteTrainTypeNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/trainservice/trains/{id}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteTrainTypeMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/trainservice/trains/{id}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
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
