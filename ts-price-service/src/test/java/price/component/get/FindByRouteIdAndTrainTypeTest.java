package price.component.get;

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
import price.entity.PriceConfig;
import price.repository.PriceConfigRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindByRouteIdAndTrainTypeTest {

    @Autowired
    private PriceConfigRepository priceConfigRepository;

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
        priceConfigRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the routeId and trainType match a PriceConfig in the database
     * <li><b>Parameters:</b></li> A routeId and trainType that matches the routeId and trainType of a PriceConfig in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the found PriceConfig object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeCorrectObject() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setRouteId("1");
        priceConfig.setTrainType("2");
        priceConfigRepository.save(priceConfig);

        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.routeId", is(priceConfig.getRouteId())))
                .andExpect(jsonPath("$.data.trainType", is(priceConfig.getTrainType())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when neither routeId nor trainType match a PriceConfig in the database
     * <li><b>Parameters:</b></li> Some random routeId and trainType that do not match any PriceConfig in the database
     * <li><b>Expected result:</b></li> status 0, msg "No that config", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeNotExists() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No that config")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when only one of the routeId or trainType match a PriceConfig in the database
     * <li><b>Parameters:</b></li> A routeID that matches the routeId of a PriceConfig in the database, and a random trainType
     * <li><b>Expected result:</b></li> status 0, msg "No that config", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeTrainTypeNotFound() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setRouteId("1");
        priceConfig.setTrainType("2");
        priceConfigRepository.save(priceConfig);

        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No that config")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when only one of the routeId or trainType match a PriceConfig in the database
     * <li><b>Parameters:</b></li> A trainType that matches the trainType of a PriceConfig in the database, and a random routeId
     * <li><b>Expected result:</b></li> status 0, msg "No that config", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeRouteIdNotFound() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setRouteId("1");
        priceConfig.setTrainType("2");
        priceConfigRepository.save(priceConfig);

        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", UUID.randomUUID().toString(), "2"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No that config")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more variables in the URL than it expects
     * <li><b>Parameters:</b></li> A routeId and trainType that matches the routeId and trainType of a PriceConfig in the database, and two random UUIDs
     * <li><b>Expected result:</b></li> status 1, msg "Success", the found PriceConfig object. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeMultipleVariables() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setRouteId("1");
        priceConfig.setTrainType("2");
        priceConfigRepository.save(priceConfig);

        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2", UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.routeId", is(priceConfig.getRouteId())))
                .andExpect(jsonPath("$.data.trainType", is(priceConfig.getTrainType())));
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
    public void testFindByRouteIdAndTrainTypeMissingVariable() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}",UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are multiple PriceConfigs with the same routeId and trainType in the database
     * <li><b>Parameters:</b></li> a routeId and trainType that matches the routeId and trainType of multiple PriceConfigs in the database
     * <li><b>Expected result:</b></li> A NestedServletException because the findByRouteIdAndTrainType is not supposed to return multiple objects.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindByRouteIdAndTrainTypeMultipleMatchingPriceConfigs() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            PriceConfig priceConfig = new PriceConfig();
            priceConfig.setId(UUID.randomUUID());
            priceConfig.setRouteId("1");
            priceConfig.setTrainType("2");
            priceConfigRepository.save(priceConfig);
        }

        // Act & Assert
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/api/v1/priceservice/prices/{routeId}/{trainType}", "1", "2")
            );
        });
    }
}
