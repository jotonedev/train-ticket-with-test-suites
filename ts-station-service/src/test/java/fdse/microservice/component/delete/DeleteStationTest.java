package fdse.microservice.component.delete;

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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteStationTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Station that does exist in the database
     * <li><b>Parameters:</b></li> A Station with an ID that matches the ID of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete success", the Deleted Station. The database no longer contains the Deleted Station.
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteValidObject() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        Station stationToDelete = new Station();
        stationToDelete.setId(storedStation.getId());
        stationToDelete.setName(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(stationToDelete)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data.id", is(stationToDelete.getId())))
                .andExpect(jsonPath("$.data.name", is(stationToDelete.getName())))
                .andExpect(jsonPath("$.data.stayTime", is(stationToDelete.getStayTime())));

        // Make sure that the station was deleted from the database
        assertEquals(0L, stationRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Station objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteMultipleObjects() throws Exception {
        // Arrange
        String requestJson = "[{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"},{\"id\":\"1\", \"name\":\"name\", \"stayTime\":\"0\"}]";

        // Act and Assert
        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Station that does not already exists in the database
     * <li><b>Parameters:</b></li> a Station with a random id
     * <li><b>Expected result:</b></li> status 0, msg "Station not exist", no data
     * <li><b>Related Issue:</b></li> <b>F5b:</b> We're performing a null check where {@code repository.findById(id)}
     * returns an Optional. This results in {@code null != Optional.empty} to always be true. The service will therefore always
     * try to Delete a station and also return Delete success even if the station does not exist in the database.<br>
     * <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteNewStationNotExists() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, stationRepository.count());

        String id = UUID.randomUUID().toString();

        Station stationToDelete = new Station();
        stationToDelete.setId(id);
        stationToDelete.setName(UUID.randomUUID().toString());

        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(stationToDelete)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Station not exist")))
                .andExpect(jsonPath("$.data", nullValue()));

        // Make sure that the database was unchanged
        assertEquals(1L, stationRepository.count());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":not, \"name\":valid, \"stayTime\":null}";

        // Act & Assert
        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a 4xx client error.
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act & Assert
        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(delete("/api/v1/stationservice/stations")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to pass a null value for the ID of the Station we want to Delete
     * <li><b>Parameters:</b></li> Station with null value for id
     * <li><b>Expected result:</b></li> an exception
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    void testDeleteNullValueForId() {
        // Arrange
        Station station = new Station(null, null);
        stationRepository.save(station);
        String requestJson = "{\"id\":null, \"name\":null, \"stayTime\":\"0\"}";

        // Act & Assert
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(delete("/api/v1/stationservice/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Station that does exist in the database and
     * try to Delete its Staytime to the smallest possible integer value
     * <li><b>Parameters:</b></li> A Station with a random ID and a Staytime of Integer.MIN_VALUE
     * <li><b>Expected result:</b></li> status 1, msg "Delete success", the Deleted Station. The database contains the Deleted Station.
     * <li><b>Related Issue:</b></li> <b>D7:</b> This endpoint is implemented to take a request body rather than a path variable as input.
     * However, including a request body in a DELETE request should be avoided, as the HTTP/1.1 specification does not define
     * semantics for payloads in DELETE methods, potentially leading to unexpected behavior.
     * </ul>
     * @throws Exception
     */
    @Test
    void testDeleteStaytimeSmallest() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        Station stationToDelete = new Station();
        stationToDelete.setId(storedStation.getId());
        stationToDelete.setStayTime(Integer.MIN_VALUE);

        // Act
        mockMvc.perform(delete("/api/v1/stationservice/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(stationToDelete)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data.id", is(stationToDelete.getId())));

        // Make sure that the station was deleted from the database
        assertEquals(0L, stationRepository.count());
    }

    private Station populateDatabase() {
        Station station = new Station();
        station.setId(UUID.randomUUID().toString());
        station.setName(UUID.randomUUID().toString());
        return stationRepository.save(station);
    }
}
