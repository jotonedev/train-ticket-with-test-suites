package price.component.put;

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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpdatePriceConfigTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with valid values and an
     * id that matches an object in the repository
     * <li><b>Parameters:</b></li> A PriceConfig object with valid values for all attributes and an id that matches an object in the repository
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigCorrectObject() throws Exception {
        // Arrange
        PriceConfig priceConfig = new PriceConfig();
        UUID id = UUID.randomUUID();
        priceConfig.setId(id);
        priceConfig.setTrainType("old train");
        priceConfig.setRouteId("old id");
        System.out.println(priceConfig);
        priceConfigRepository.save(priceConfig);
        assertEquals(1, priceConfigRepository.findAll().size());

        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"new train\"," +
                " \"routeId\":\"new id\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("new train")))
                .andExpect(jsonPath("$.data.routeId", is("new id")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(0.0)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(0.0)));

        // Make sure the object was updated in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("new train", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("new id", priceConfigRepository.findAll().get(0).getRouteId());
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
    public void testUpdatePriceConfigMultipleObjects() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson =
                "[{\"id\":\"" + id + "\"," +
                " \"trainType\":\"train\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}," +

                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"train\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}]";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
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
    public void testUpdatePriceConfigMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
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
    public void testUpdatePriceConfigMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig where the id does not
     * match any object in the repository
     * <li><b>Parameters:</b></li> A PriceConfig object with a random id
     * <li><b>Expected result:</b></li> status 0, msg "No that config", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigRandomNotExists() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                " \"trainType\":\"new train\"," +
                " \"routeId\":\"new id\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No that config")))
                .andExpect(jsonPath("$.data", is(nullValue())));
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
    public void testUpdatePriceConfigNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/priceservice/prices")
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
    public void testUpdatePriceConfigInvalidId() throws Exception {
        // Arrange
        String requestJson =
                "[{\"id\":\"not a valid UUID\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"0.0\"," +
                        " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
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
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig. The id will be the same as the one given in the request
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigMaxUUID() throws Exception {
        UUID id = UUID.fromString("fffffff-ffff-ffff-ffff-ffffffffffff");
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"0.0\"," +
                        " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
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
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig. The id will be the same as the one given in the request
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigMinUUID() throws Exception {
        // Arrange
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"0.0\"," +
                        " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig where the id is null
     * <li><b>Parameters:</b></li> A PriceConfig object with null for the id
     * <li><b>Expected result:</b></li> status 0, msg "No that config", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigIdNull() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":null," +
                " \"trainType\":\"1\"," +
                " \"routeId\":\"1\"," +
                " \"basicPriceRate\":\"0.0\"," +
                " \"firstClassPriceRate\":\"0.0\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No that config")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint a PriceConfig object with values for the string attributes that are null
     * <li><b>Parameters:</b></li> A PriceConfig object with null for trainType and routeId
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig. The string attributes will be null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigNullValuesForStringAttributes() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":null," +
                        " \"routeId\":null," +
                        " \"basicPriceRate\":\"0.0\"," +
                        " \"firstClassPriceRate\":\"0.0\"}";


        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
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
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig. The number attributes will be the maximum possible value.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigMaxValuesForNumberAttributes() throws Exception{
        // Arrange
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        double value = Double.MAX_VALUE;
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"" + value + "\"," +
                        " \"firstClassPriceRate\":\"" + value + "\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")));

        // Make sure the object was updated in the repository
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
     * <li><b>Expected result:</b></li> status 1, msg "Update success", the updated PriceConfig. The number attributes will be the minimum possible value.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testUpdatePriceConfigMinValuesForNumberAttributes() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        PriceConfig price = new PriceConfig();
        price.setId(id);
        priceConfigRepository.save(price);
        double value = Double.MIN_VALUE;
        String requestJson =
                "{\"id\":\"" + id + "\"," +
                        " \"trainType\":\"1\"," +
                        " \"routeId\":\"1\"," +
                        " \"basicPriceRate\":\"" + value + "\"," +
                        " \"firstClassPriceRate\":\"" + value + "\"}";

        // Act
        mockMvc.perform(put("/api/v1/priceservice/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainType", is("1")))
                .andExpect(jsonPath("$.data.routeId", is("1")))
                .andExpect(jsonPath("$.data.basicPriceRate", is(value)))
                .andExpect(jsonPath("$.data.firstClassPriceRate", is(value)));

        // Make sure the object was updated in the repository
        assertNotNull(priceConfigRepository.findById(id));
        assertEquals("1", priceConfigRepository.findAll().get(0).getTrainType());
        assertEquals("1", priceConfigRepository.findAll().get(0).getRouteId());
        assertEquals(value, priceConfigRepository.findAll().get(0).getBasicPriceRate());
        assertEquals(value, priceConfigRepository.findAll().get(0).getFirstClassPriceRate());
    }
}
