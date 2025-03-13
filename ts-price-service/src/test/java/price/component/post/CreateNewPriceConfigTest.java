package price.component.post;

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
import price.entity.PriceConfig;
import price.repository.PriceConfigRepository;

import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CreateNewPriceConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that is null
     * <li><b>Parameters:</b></li> A PriceConfig object with null as id
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. A new ID will be generated
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigSuccessfulCreationNullId() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":null," +
                " \"trainType\":\"train\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                // Id will be randomly generated, meaning it is no longer null
                .andExpect(jsonPath("$.data.id", not(nullValue())))
                .andExpect(jsonPath("$.data.trainType", is("train")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was saved in the repository
        assertEquals(1, priceConfigRepository.findAll().size());
        assertEquals("train", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Price Config objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigMultipleObjects() throws Exception {
        // Arrange
        String requestJson = "[{\"id\":null," +
                " \"trainType\":\"train\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}," +

                "{\"id\":null," +
                " \"trainType\":\"train\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}]";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that is defined, but not in the repository
     * <li><b>Parameters:</b></li> A PriceConfig object with a defined id
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The id will be the same as the one given in the request
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigSuccessfulCreationDefinedId() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"train\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"0.0\"," +
                        " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("train")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was saved in the repository
        assertEquals(1, priceConfigRepository.findAll().size());
        assertEquals("train", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that already exists in the repository
     * <li><b>Parameters:</b></li> A PriceConfig object with an id that already exists in the repository
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the updated PriceConfig. The PriceConfig object that was
     * in the repository has been updated to the new values given in the request.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigSuccessfulModification() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        priceConfig.setId(UUID.randomUUID());
        priceConfig.setTrainType("train");
        priceConfig.setRouteId("1");
        priceConfig.setBasicPriceRate(0.0);
        priceConfig.setFirstClassPriceRate(0.0);
        priceConfigRepository.save(priceConfig);

        String requestJson =
                "{\"id\":\"" + priceConfig.getId() + "\"," +
                        " \"trainType\":\"newTrain\"," +
                        " \"routeId\":\"2\"," +
                        " \"basicPriceRate\":\"5.0\"," +
                        " \"firstClassPriceRate\":\"5.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(priceConfig.getId().toString())))
                .andExpect(jsonPath("$.data.trainType", is("newTrain")))
                .andExpect(jsonPath("$.data.routeId", is("2")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(5.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(5.0)));

        // Make sure the object was updated in the repository
        assertEquals(1, priceConfigRepository.findAll().size());
        assertEquals("newTrain", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("2", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(5.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(5.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
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
    public void testCreateNewPriceConfigMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
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
    public void testCreateNewPriceConfigMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
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
    public void testCreateNewPriceConfigNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/priceservice/prices")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that is not a valid UUID
     * <li><b>Parameters:</b></li> A PriceConfig object with an invalid id
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigInvalidId() throws Exception {
        // Arrange
        String requestJson =
                "[{\"id\":\"not a valid UUID\"," +
                " \"trainType\":\"1\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that
     * represents the maximum value of a UUID
     * <li><b>Parameters:</b></li> A PriceConfig object with a UUID that is the maximum value
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The id will be the same as the one given in the request
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigMaxUUID() throws Exception {
        // Arrange
        UUID id = UUID.fromString("fffffff-ffff-ffff-ffff-ffffffffffff");
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"1\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was saved in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with an id that
     * represents the minimum value of a UUID
     * <li><b>Parameters:</b></li> A PriceConfig object with a UUID that is the minimum value
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The id will be the same as the one given in the request
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigMinUUID() throws Exception {
        // Arrange
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"1\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was saved in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with values for the string attributes that are null
     * <li><b>Parameters:</b></li> A PriceConfig object with null for trainType and routeId
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The string attributes will be null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigNullValuesForStringAttributes() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":null," +
                " \"routeId\":null," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";


        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is(nullValue())))
                .andExpect(jsonPath("$.data.routeId", is(nullValue())))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was saved in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertNull(priceConfigRepository.findAll().get(0).getTrainType());
        assertNull(priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(0.0, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with values for the number attributes
     * which have the maximum possible value
     * <li><b>Parameters:</b></li> A PriceConfig object with the maximum possible value for basicPriceRate and firstClassPriceRate
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The number attributes will be the maximum possible value.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigMaxValuesForNumberAttributes() throws Exception{
        UUID id = UUID.randomUUID();
        double value = Double.MAX_VALUE;
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"1\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"" + value + "\"," +
                " \"firstClassPriceRate\":\"" + value + "\"}";


        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")));

        // Make sure the object was saved in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(value, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with values for the number attributes
     * which have the minimum possible value
     * <li><b>Parameters:</b></li> A PriceConfig object with the minimum possible value for basicPriceRate and firstClassPriceRate
     * <li><b>Expected result:</b></li> status 1, msg "Create success", the created PriceConfig. The number attributes will be the minimum possible value.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCreateNewPriceConfigMinValuesForNumberAttributes() throws Exception{
        UUID id = UUID.randomUUID();
        double value = Double.MIN_VALUE;
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"" + value + "\"," +
                        " \"firstClassPriceRate\":\"" + value + "\"}";


        // Act
        mockMvc.perform(post("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Create success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(value)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(value)));

        // Make sure the object was saved in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(value, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }
}
