package route.component.get;

import com.alibaba.fastjson.JSONObject;
import edu.fudan.common.util.Response;
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
import route.entity.Route;
import route.repository.RouteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetRouteByStartAndTerminalTest {

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
     * <li><b>Tests:</b></li> that all stored Routes are returned on endpoint call
     * <li><b>Parameters:</b></li> startId and terminalId of the route that exist in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all routes with the startId and terminalId
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalAllObjects() throws Exception {
        // Arrange
        List<String> stations = new ArrayList<>();
        String startId = UUID.randomUUID().toString();
        String terminalId = UUID.randomUUID().toString();
        stations.add(startId);
        stations.add(terminalId);
        for (int i = 0; i < 1000; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", startId, terminalId))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1000)))
                .andExpect(jsonPath("$.data[0].stations[0]", is(startId)))
                .andExpect(jsonPath("$.data[0].stations[1]", is(terminalId)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No routes with the startId and terminalId", null
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No routes with the startId and terminalId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database does have a few routes, but there is no station
     * with the id in any of the stationLists
     * <li><b>Parameters:</b></li> some random string as for either startId or terminalId
     * <li><b>Expected result:</b></li> status 0, msg "No routes with the startId and terminalId", null
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalNotMatchingId() throws Exception {
        // Arrange
        List<String> stations = new ArrayList<>();
        String startId = UUID.randomUUID().toString();
        String terminalId = UUID.randomUUID().toString();
        stations.add(startId);
        stations.add(terminalId);
        for (int i = 0; i < 10; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", startId, UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No routes with the startId and terminalId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testGetRouteByStartAndTerminalNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all routes with the startId and terminalId.
     * Only the first two ids are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalMultipleIds() throws Exception {
        // Arrange
        List<String> stations = new ArrayList<>();
        String startId = UUID.randomUUID().toString();
        String terminalId = UUID.randomUUID().toString();
        stations.add(startId);
        stations.add(terminalId);
        for (int i = 0; i < 10; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", startId, terminalId, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.data[0].stations[0]", is(startId)))
                .andExpect(jsonPath("$.data[0].stations[1]", is(terminalId)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give an id, which is not the correct format for an id.
     * <li><b>Parameters:</b></li> some badly formatted id
     * <li><b>Expected result:</b></li> status 0, msg "No routes with the startId and terminalId", no data. The id as URL parameter
     * is a String with no length restrictions, therefore it is not checked for a correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalIncorrectFormatId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "not a correct start id", "not a correct terminal id"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No routes with the startId and terminalId")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that only some stored Routes are returned on endpoint call
     * <li><b>Parameters:</b></li> startId and terminalId of the route that exist in the database for some routes, and is in
     * the wrong order in the stationList for some other routes
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of some routes with the startId and terminalId
     * @throws Exception
     */
    @Test
    public void testGetRouteByStartAndTerminalMixedStations() throws Exception {
        // Arrange
        List<String> stations = new ArrayList<>();
        stations.add("1");
        stations.add("2");
        for (int i = 0; i < 5; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }
        stations.remove(0);
        stations.add("1");
        assertEquals(1, stations.indexOf("1"));
        for (int i = 5; i < 10; i++) {
            Route route = new Route();
            route.setId(String.valueOf(i));
            route.setStations(stations);
            routeRepository.save(route);
        }

        // Act
        mockMvc.perform(get("/api/v1/routeservice/routes/{startId}/{terminalId}", "1", "2"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].stations[0]", is("1")))
                .andExpect(jsonPath("$.data[0].stations[1]", is("2")));
    }
}
