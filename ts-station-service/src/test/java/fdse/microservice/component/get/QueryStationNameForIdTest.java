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
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
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
public class QueryStationNameForIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a Station with a name that matches the name of a Station in the database
     * <li><b>Parameters:</b></li> A Station with a name that matches the name of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the station ID corresponding to the name in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameForIdElementInDatabase() throws Exception {
        // Arrange
        Station station = new Station(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        stationRepository.save(station);

        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}", station.getName()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(station.getId())));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one name as the URL parameter
     * <li><b>Parameters:</b></li> A Station with a name that matches the name of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the station ID corresponding to the name in the database
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameForIdMultipleNames() throws Exception {
        // Arrange
        Station station = new Station(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        stationRepository.save(station);

        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}", station.getName(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(station.getId())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a Station with a name that does not match the name of a Station in the database
     * <li><b>Parameters:</b></li> A Station with a name that does not match the name of a Station in the database
     * <li><b>Expected result:</b></li> status 1, msg "Not exists", the provided stationName
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStationNameForIdNotExists() throws Exception {
        // Act
        String stationName = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}", stationName))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Not exists")))
                .andExpect(jsonPath("$.data", is(stationName)));
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
    public void TestQueryStationNameForIdMissingName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testQueryStationNameForIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/stationservice/stations/id/{stationNameForId}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().is4xxClientError());
    }
}
