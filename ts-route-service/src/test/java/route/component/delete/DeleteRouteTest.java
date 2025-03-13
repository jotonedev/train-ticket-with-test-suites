package route.component.delete;

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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteRouteTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to Delete a Route that does exist in the database
     * <li><b>Parameters:</b></li> An Route with an ID that matches the ID of a Route in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the Deleted routeId. The database no longer contains that Route.
     * </ul>
     * @throws Exception
     */
    @Test
    void testDeleteRouteMatchingId() throws Exception {
        // Arrange
        Route route = new Route();
        routeRepository.save(route);
        assertTrue(routeRepository.findById(route.getId()).isPresent());

        // Act
        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", route.getId()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success")))
                .andExpect(jsonPath("$.data", is(route.getId())));

        assertFalse(routeRepository.findById(route.getId()).isPresent());

    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id given
     * in paths
     * <li><b>Parameters:</b></li> some random id that does not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the provided id. The database remains unchanged.
     * <li><b>Related Issue:</b></li> <b>D1:</b> repository.delete() is performed before the check whether the object
     * exists, always resulting in a successful deletion.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteRouteNotMatchingId() throws Exception {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act
        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", randomId.toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success")))
                .andExpect(jsonPath("$.data", is(randomId.toString())));

        assertFalse(routeRepository.findById("1").isPresent());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the deleted routeId. Only the first id in paths is used to delete the route.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteRouteMultipleIds() throws Exception {
        // Arrange
        Route route = new Route();
        routeRepository.save(route);
        assertTrue(routeRepository.findById(route.getId()).isPresent());

        // Act
        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", route.getId(), UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success")))
                .andExpect(jsonPath("$.data", is(route.getId())));

        assertFalse(routeRepository.findById(route.getId()).isPresent());
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteRouteMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
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
     *
     * @throws Exception
     */
    @Test
    public void testDeleteRouteNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give an id, which is not the correct format for an id.
     * <li><b>Parameters:</b></li> some badly formatted id
     * <li><b>Expected result:</b></li> status 1, msg "Delete Success", the provided malformed id. The id as URL parameter
     * is a String with no length restrictions, therefore it is not checked for a correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    void testDeleteRouteIncorrectFormatId() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/routeservice/routes/{routeId}", "not a correct format id"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Success")))
                .andExpect(jsonPath("$.data", is("not a correct format id")));
    }
}
