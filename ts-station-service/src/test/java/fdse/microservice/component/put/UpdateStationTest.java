package fdse.microservice.component.put;

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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdateStationTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a Station that does exist in the database
     * <li><b>Parameters:</b></li> A Station with an ID that matches the ID of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated Station. The database contains the updated Station.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateValidObject() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        Station updatedStation = new Station();
        updatedStation.setId(storedStation.getId());
        updatedStation.setName(UUID.randomUUID().toString());
        updatedStation.setStayTime(123);

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.id", is(updatedStation.getId())))
                .andExpect(jsonPath("$.data.name", is(updatedStation.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(updatedStation.getStayTime())));

        // Make sure that the Station was updated in the database
        Optional<Station> optionalUpdatedStation = stationRepository.findById(updatedStation.getId());
        assertTrue(optionalUpdatedStation.isPresent());
        Station savedStation = optionalUpdatedStation.get();
        assertEquals(updatedStation.getId(), savedStation.getId());
        assertEquals(updatedStation.getName(), savedStation.getName());
        assertEquals(updatedStation.getStayTime(), savedStation.getStayTime());
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
    public void testUpdateMultipleObjects() throws Exception {
        // Arrange
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"1\"},{\"id\":\"2\", \"name\":\"name2\", \"stayTime\":\"1\"}]";

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a Station that does not already exists in the database
     * <li><b>Parameters:</b></li> a Station with a random id
     * <li><b>Expected result:</b></li> status 0, msg "Station not exist", no data
     * <li><b>Related Issue:</b></li> <b>F5b:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null == Optional.empty} to always be false. The service will therefore always
     * try to update a station and also return update success even if the station does not exist in the database.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testUpdateNewStationNotExists() throws Exception {
        // Arrange
        Station newStation = new Station();
        newStation.setId(UUID.randomUUID().toString());
        newStation.setName(UUID.randomUUID().toString());
        newStation.setStayTime(123);

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Station not exist")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    public void testUpdateMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
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
    public void invalidTestMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
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
    public void testUpdateNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/stationservice/stations")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to pass a null value for the ID of the Station we want to update
     * <li><b>Parameters:</b></li> Station with null value for id
     * <li><b>Expected result:</b></li> an exception
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateNullValueForId() {
        // Arrange
        Station station = new Station(null, null);
        stationRepository.save(station);
        String requestJson = "{\"id\":null, \"name\":\"nameNew\", \"stayTime\":\"0\"}";

        // Act & Assert
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(put("/api/v1/stationservice/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to update a Station that does exist in the database and
     * try to update its Staytime to the smallest possible integer value
     * <li><b>Parameters:</b></li> A Station with a random ID and a Staytime of Integer.MIN_VALUE
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated Station. The database contains the updated Station.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdateStaytimeSmallest() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        Station updatedStation = new Station();
        updatedStation.setId(storedStation.getId());
        updatedStation.setName(UUID.randomUUID().toString());
        updatedStation.setStayTime(Integer.MIN_VALUE);

        // Act
        mockMvc.perform(put("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedStation)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.id", is(updatedStation.getId())))
                .andExpect(jsonPath("$.data.name", is(updatedStation.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(Integer.MIN_VALUE)));

        // Make sure that the Station was updated in the database
        Optional<Station> optionalUpdatedStation = stationRepository.findById(updatedStation.getId());
        assertTrue(optionalUpdatedStation.isPresent());
        Station savedStation = optionalUpdatedStation.get();
        assertEquals(updatedStation.getId(), savedStation.getId());
        assertEquals(updatedStation.getName(), savedStation.getName());
        assertEquals(updatedStation.getStayTime(), savedStation.getStayTime());
    }

    private Station populateDatabase() {
        Station station = new Station();
        station.setId(UUID.randomUUID().toString());
        station.setName(UUID.randomUUID().toString());
        return stationRepository.save(station);
    }
}
