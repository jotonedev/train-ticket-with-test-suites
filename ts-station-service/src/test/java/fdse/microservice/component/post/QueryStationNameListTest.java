package fdse.microservice.component.post;

import com.alibaba.fastjson.JSONObject;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryStationNameListTest {
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains all elements that match each id of
     * any element within the list of id in the request body
     * <li><b>Parameters:</b></li> a list including only one id that matches the id of the element stored in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a list of names corresponding to the ids in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListCorrectObject() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        ArrayList<String> idList = new ArrayList<>();
        idList.add(storedStation.getId());

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(idList)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0]", is(storedStation.getName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> A list containing two lists of Strings
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListMultipleObjects() throws Exception {
        // Arrange
        List<List<String>> lists = new ArrayList<>();
        lists.add(new ArrayList<>());
        lists.add(new ArrayList<>());
        String requestJson = JSONObject.toJSONString(lists);

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testQueryStationNameListMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{[not, a, valid, json]}";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
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
    public void testQueryStationNameListMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
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
    public void testQueryStationNameListNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when there are two Stations without an id with two different names in the database
     * <li><b>Parameters:</b></li> A list containing one null-element to represent no id.
     * <li><b>Expected result:</b></li> an error
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListDuplicateTestString() {
        // Arrange
        List<String> idList = new ArrayList<>();
        idList.add(null);
        Station station = new Station(null, "name");
        stationRepository.save(station);
        station.setName("name2");
        stationRepository.save(station);
        String requestJson = JSONObject.toJSONString(idList);

        // Act & Assert
        assertThrows(NestedServletException.class, () ->
                mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the list of station ids provided in the request body contains duplicate
     * station ids that match an element within the database
     * <li><b>Parameters:</b></li> a list including two elements where both match the same id of the element stored in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a created list of all matching station names where two or more elements
     * refer to the same station by having equal station names.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListDuplicateNamesInList() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        ArrayList<String> idList = new ArrayList<>();
        idList.add(storedStation.getId());
        idList.add(storedStation.getId());

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(idList)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0]", is(storedStation.getName())))
                .andExpect(jsonPath("$.data[1]", is(storedStation.getName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the list of station ids provided in the request body contains both ids
     * that are in the database and ids which aren’t
     * <li><b>Parameters:</b></li> a list including both elements that match the name of the element stored in the database and elements
     * that do not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a created list of all matching station ids as well as at
     * least one element of “Not Exist”.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListMixedList() throws Exception {
        // Arrange
        Station storedStation = populateDatabase();
        assertEquals(1L, stationRepository.count());

        ArrayList<String> idList = new ArrayList<>();
        idList.add(UUID.randomUUID().toString());
        idList.add(storedStation.getId());

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(idList)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0]", is(storedStation.getName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database no elements that match each id of any element
     * within the list of ids in the request body
     * <li><b>Parameters:</b></li> a list including random Strings
     * <li><b>Expected result:</b></li> status 0, msg "No stationNamelist according to stationIdList", empty list
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListEmptyList() throws Exception {
        // Arrange
        ArrayList<String> idList = new ArrayList<>();

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(idList)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No stationNamelist according to stationIdList")))
                .andExpect(jsonPath("$.data", is(empty())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database no elements that match each id of any element
     * within the list of ids in the request body
     * <li><b>Parameters:</b></li> a list including random Strings
     * <li><b>Expected result:</b></li> status 0, msg "No stationNamelist according to stationIdList", empty list
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameListNotExists() throws Exception {
        // Arrange
        ArrayList<String> idList = new ArrayList<>();
        idList.add(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/stationservice/stations/namelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(idList)))
                .andDo(print())
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No stationNamelist according to stationIdList")))
                .andExpect(jsonPath("$.data", is(empty())));
    }

    private Station populateDatabase() {
        Station station = new Station();
        station.setId(UUID.randomUUID().toString());
        station.setName(UUID.randomUUID().toString());
        return stationRepository.save(station);
    }
}