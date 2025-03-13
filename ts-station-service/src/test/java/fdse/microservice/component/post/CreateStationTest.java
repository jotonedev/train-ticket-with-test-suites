package fdse.microservice.component.post;

import fdse.microservice.entity.Station;
import fdse.microservice.repository.StationRepository;
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

import java.util.Objects;
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
public class CreateStationTest {

    @Autowired
    private StationRepository stationRepository;

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
        stationRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Station that does not yet exist in the database
     * <li><b>Parameters:</b></li> A Station with a random ID
     * <li><b>Expected result:</b></li> status 1, msg "create success", the database contains the new Station
     * <li><b>Related Issue:</b></li> <b>F5b:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null == Optional.empty} to always be false. The service will therefore never
     * create a new Object, because it thinks that it already exists in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateValidObject() throws Exception {
        // Arrange
        Station newStation = new Station();
        newStation.setId(UUID.randomUUID().toString());
        newStation.setName(UUID.randomUUID().toString());
        newStation.setStayTime(123);

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(newStation.getId())))
                .andExpect(jsonPath("$.data.name", is(newStation.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(newStation.getStayTime())));

        // Make sure that the train type was saved to the database
        assertEquals(1L, stationRepository.count());
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
    public void testCreateMultipleObjects() throws Exception {
        // Arrange
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"1\"},{\"id\":\"2\", \"name\":\"name2\", \"stayTime\":\"1\"}]";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Station that already exists in the database
     * <li><b>Parameters:</b></li> a Station with an id that matches the id in the database
     * <li><b>Expected result:</b></li> status 0, msg "Already exists", the provided Station. The database remains unchanged.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewStationAlreadyExists() throws Exception {
        // Arrange
        Station existingStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(existingStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Already exists")))
                .andExpect(jsonPath("$.data.id", is(existingStation.getId())))
                .andExpect(jsonPath("$.data.name", is(existingStation.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(existingStation.getStayTime())));

        // Make sure that the database was unchanged
        assertEquals(1L, stationRepository.count());
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
    public void testCreateMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
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
    void invalidTestMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
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
    public void testCreateNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Station object with null values for all attributes, since
     * id and name are Strings without restrictions
     * <li><b>Parameters:</b></li> A Station with a random ID
     * <li><b>Expected result:</b></li> status 1, msg "create success", the database contains the new Station
     * <li><b>Related Issue:</b></li> <b>F5b:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null == Optional.empty} to always be false. The service will therefore never
     * create a new Object, because it thinks that it already exists in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCreateNullAttributes() throws Exception {
        // Arrange
        Station newStation = new Station();

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(newStation.getId())))
                .andExpect(jsonPath("$.data.name", is("")))
                .andExpect(jsonPath("$.data.stayTime", is(0)));

        // Make sure that the train type was saved to the database
        assertEquals(1L, stationRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to create a Station with a Staytime value that is the smallest possible integer value
     * <li><b>Parameters:</b></li> A Station with a random ID and the smallest possible integer value for stayTime
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the database contains the new Station
     * <li><b>Related Issue:</b></li> <b>F5b:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null == Optional.empty} to always be false. The service will therefore never
     * create a new Object, because it thinks that it already exists in the database
     * </ul>
     * @throws Exception
     */
    @Test
    void FAILING_testCreateStaytimeSmallest() throws Exception {
        // Arrange
        Station newStation = new Station();
        newStation.setId(UUID.randomUUID().toString());
        newStation.setName(UUID.randomUUID().toString());
        newStation.setStayTime(Integer.MIN_VALUE);

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(newStation.getId())))
                .andExpect(jsonPath("$.data.name", is(newStation.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(Integer.MIN_VALUE)));

        // Make sure that the train type was saved to the database
        assertEquals(1L, stationRepository.count());
    }

    private Station populateDatabase() {
        Station station = new Station();
        station.setId(UUID.randomUUID().toString());
        station.setName(UUID.randomUUID().toString());
        return stationRepository.save(station);
    }
}
