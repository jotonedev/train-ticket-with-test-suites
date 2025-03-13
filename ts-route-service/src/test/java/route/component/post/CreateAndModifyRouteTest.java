package route.component.post;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
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
import route.entity.Route;
import route.repository.RouteRepository;

import java.util.Objects;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateAndModifyRouteTest {

    @Autowired
    private RouteRepository routeRepository;

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
    public void beforeEach() {
        routeRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we try adding a route that does not yet exist in the database
     * <li><b>Parameters:</b></li> A Route object with a unique ID
     * <li><b>Expected result:</b></li> status 1, msg "Save Success", the newly created Route object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyCorrectObject() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Save Success")))
                .andExpect(jsonPath("$.data.startStationId", is("1")))
                .andExpect(jsonPath("$.data.terminalStationId", is("2")))
                .andExpect(jsonPath("$.data.stations[0]", is("1")))
                .andExpect(jsonPath("$.data.stations[1]", is("2")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertEquals(1, routeRepository.findAll().size());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two route objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteMultipleObjects() throws Exception {
        // Arrange
        String requestJson =
                "[{\"id\":\"1\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}," +

                "{\"id\":\"1\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}]";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the route already exists in the repository
     * <li><b>Parameters:</b></li> An route object with an id that already exists in the repository
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the modified Route object. The route will be modified
     * with its new values when we try adding a route with an id that already exists in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteDuplicateObject() throws Exception {
        // Arrange
        // First endpoint call: Create a route with id "1234567890"
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        assertTrue(routeRepository.findById("1234567890").isPresent());

        // Second endpoint call: Try saving a route with the same id "1234567890" but different values otherwise
        requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"3\"," +
                " \"endStation\":\"4\"," +
                " \"stationList\":\"3,4\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("1234567890")))
                .andExpect(jsonPath("$.data.startStationId", is("3")))
                .andExpect(jsonPath("$.data.terminalStationId", is("4")))
                .andExpect(jsonPath("$.data.stations[0]", is("3")))
                .andExpect(jsonPath("$.data.stations[1]", is("4")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertTrue(routeRepository.findById("1234567890").isPresent());
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
    public void testCreateAndModifyRouteMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
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
    public void testCreateAndModifyRouteEmptyObject() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
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
    public void testCreateAccountNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/routeservice/routes")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the routeId does not yet exist in the database, but has an id with a length >= 10
     * <li><b>Parameters:</b></li> An route object with an id that has a length >= 10 and is unique
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the created Route object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteIdGreaterEqual10() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("1234567890")))
                .andExpect(jsonPath("$.data.startStationId", is("1")))
                .andExpect(jsonPath("$.data.terminalStationId", is("2")))
                .andExpect(jsonPath("$.data.stations[0]", is("1")))
                .andExpect(jsonPath("$.data.stations[1]", is("2")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertTrue(routeRepository.findById("1234567890").isPresent());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the routeId is too short or null
     * <li><b>Parameters:</b></li> An route object with an id that is null
     * <li><b>Expected result:</b></li> status 1, msg "Save Success", the created Route object with a new id.
     * </ul>
     * @throws Exception
     */
    @Test
    void testCreateAndModifyRouteIdTooShortOrNull() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":null," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Save Success")))
                .andExpect(jsonPath("$.data.startStationId", is("1")))
                .andExpect(jsonPath("$.data.terminalStationId", is("2")))
                .andExpect(jsonPath("$.data.stations[0]", is("1")))
                .andExpect(jsonPath("$.data.stations[1]", is("2")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertEquals(1, routeRepository.findAll().size());
        assertNotNull(routeRepository.findAll().get(0).getId());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the routeId is badly formatted
     * <li><b>Parameters:</b></li> some badly formatted routeId
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the provided route with its malformed id. The id
     * is a String with no length restrictions, therefore it is not checked for a correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteIncorrectFormatId() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"not a correct format id\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("not a correct format id")))
                .andExpect(jsonPath("$.data.startStationId", is("1")))
                .andExpect(jsonPath("$.data.terminalStationId", is("2")))
                .andExpect(jsonPath("$.data.stations[0]", is("1")))
                .andExpect(jsonPath("$.data.stations[1]", is("2")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertTrue(routeRepository.findById("not a correct format id").isPresent());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when startStation and endStation are empty or null
     * <li><b>Parameters:</b></li> A Route object with startStation and endStation as empty Strings or null
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the provided route object. The startStation and endStation
     * are Strings with no length restrictions, therefore they are not checked for correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteNullOrEmptyStation() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"\"," +
                " \"endStation\":null," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"0,300\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("1234567890")))
                .andExpect(jsonPath("$.data.startStationId", is("")))
                .andExpect(jsonPath("$.data.terminalStationId", nullValue()))
                .andExpect(jsonPath("$.data.stations[0]", is("1")))
                .andExpect(jsonPath("$.data.stations[1]", is("2")))
                .andExpect(jsonPath("$.data.distances[0]", is(0)))
                .andExpect(jsonPath("$.data.distances[1]", is(300)));

        assertTrue(routeRepository.findById("1234567890").isPresent());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when stationList contains inconsistent data
     * <li><b>Parameters:</b></li> A Route object with stationList containing numbers, letters and special characters
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the provided route object. The content of stationList is a String
     * with no length restrictions, therefore it is not checked for correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteStationListInconsistent() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,test,3,,()/&\"," +
                " \"distanceList\":\"1,2,3,4,5\"}";

        // Act
        String result = mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("1234567890")))
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());

        String[] stations = "1,test,3,,()/&".split(",");
        String[] distances = "1,2,3,4,5".split(",");
        Route route = new ObjectMapper().convertValue(JSONObject.parseObject(result, Response.class).getData(), Route.class);

        for(int i = 0; i < stations.length; i++) {
            assertEquals(stations[i], route.getStations().get(i));
            assertEquals(Integer.parseInt(distances[i]), route.getDistances().get(i));
        }
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when stationList and distanceList are null
     * <li><b>Parameters:</b></li> A Route object with stationList and distanceList as null
     * <li><b>Expected result:</b></li> The service should handle the case stationList and distanceList are null in some way.
     * Whether that response should involve a status of 0 or 1 is up to the implementation, but the HTTP status should be 200.
     * <li><b>Related Issue:</b></li> <b>F8c:</b> The service assumes that the object in the requestBody is fully defined
     * and uses that object without performing null-checks. This will lead to a NullPointerException that is unhandled
     * if the values within the provided object are accessed.
     * </ul>
     * @throws Exception
     */
    @Test
    void FAILING_testCreateAndModifyRouteStationListDistanceListNull() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":null," +
                " \"distanceList\":null}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                // The service implementation should handle the case where the Assurance is not fully defined in some way.
                // Whether that response should involve a status of 0 or 1 is up to the implementation, but the HTTP status
                // should be 200.
                .andExpect(status().isOk());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when stationList and distanceList are empty
     * <li><b>Parameters:</b></li> A Route object with and empty stationList and distanceList
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the provided route object. The content of stationList and distanceList
     * is empty.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteEmptyList() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\",\"," +
                " \"distanceList\":\",\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is("1234567890")))
                .andReturn().getResponse().getContentAsString();


        assertTrue(routeRepository.findById("1234567890").isPresent());
        assertEquals(0, routeRepository.findById("1234567890").get().getDistances().size());
        assertEquals(0, routeRepository.findById("1234567890").get().getStations().size());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when stationList and distanceList have different lengths
     * <li><b>Parameters:</b></li> A Route object with stationList and distanceList having different lengths
     * <li><b>Expected result:</b></li> status 0, msg "Station Number Not Equal To Distance Number", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreatAndModifyListsDifferentLengthsTest() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"1,2\"," +
                " \"distanceList\":\"2\"}";

        // Act
        mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Station Number Not Equal To Distance Number")))
                .andExpect(jsonPath("$.data", nullValue()));

        assertFalse(routeRepository.findById("1234567890").isPresent());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when stationList and / or distanceList have invalid characters
     * <li><b>Parameters:</b></li> A Route object with stationList and distanceList containing invalid characters
     * <li><b>Expected result:</b></li> a NestedServletException with the message "not a valid distance list"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateAndModifyRouteListInvalidCharactersTest() {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"startStation\":\"1\"," +
                " \"endStation\":\"2\"," +
                " \"stationList\":\"not a valid station list\"," +
                " \"distanceList\":\"not a valid distance list\"}";

        Exception exception = assertThrows(NestedServletException.class,
                // Act
                () -> mockMvc.perform(post("/api/v1/routeservice/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)));

        // Assert
        assertTrue(Objects.requireNonNull(exception.getMessage()).contains("not a valid distance list"));
    }
}
