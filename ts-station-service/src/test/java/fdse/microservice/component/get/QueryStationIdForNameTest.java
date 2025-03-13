package fdse.microservice.component.get;

import fdse.microservice.entity.Station;
import fdse.microservice.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
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
public class QueryStationIdForNameTest {
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a Station with an ID that matches the ID of a Station in the database
     * <li><b>Parameters:</b></li> A Station with a name that matches the name of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the station ID corresponding to the name in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationIdForNameElementInDatabase() throws Exception {
        // Arrange
        Station station = new Station(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        stationRepository.save(station);

        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationIdForName}", station.getName()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(station.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", the station ID corresponding to the name in the database. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationIdForNameMultipleId() throws Exception {
        // Arrange
        Station station = new Station(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        stationRepository.save(station);

        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationIdForName}", station.getName(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(station.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a Station with an ID that does not match the ID of a Station in the database
     * <li><b>Parameters:</b></li> A Station with a name that does not match the name of a Station in the database
     * <li><b>Expected result:</b></li> status 0, msg "Not exists", the provided stationId
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationIdForNameNotExists() throws Exception {
        // Act
        String stationId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationIdForName}", stationId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Not exists")))
                .andExpect(jsonPath("$.data", is(stationId)));
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
    public void testQueryStationIdForNameMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationIdForName}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testQueryStationIdForNameMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationIdForName}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().is4xxClientError());
    }
}
