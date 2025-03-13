package payment.component.post;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Iterables;
import com.trainticket.PaymentApplication;
import com.trainticket.entity.Money;
import com.trainticket.repository.AddMoneyRepository;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PaymentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AddMoneyTest {

    @Autowired
    private AddMoneyRepository addMoneyRepository;

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
        addMoneyRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give it a correct object
     * <li><b>Parameters:</b></li> a payment object with valid values for all attributes
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyCorrectObject() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"orderId\":\"1\"," +
                " \"userId\":\"1\"," +
                " \"price\":\"1\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is("1")))
                .andExpect(jsonPath("$.data.money", is("1")));

        // Make sure the money object was saved in the repository
        assertEquals(1, Iterables.size(addMoneyRepository.findAll()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Payment objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyMultipleObjects() throws Exception {
        // Arrange
        String requestJson = "[{\"id\":\"1234567890\"," +
                " \"orderId\":\"1\"," +
                " \"userId\":\"1\"," +
                " \"price\":\"1\"}," +

                "{\"id\":\"id2\"," +
                " \"orderId\":\"2\"," +
                " \"userId\":\"2\"," +
                " \"price\":\"1\"}]";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
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
    public void testAddMoneyMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
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
    public void testAddMoneyMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
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
    public void testAddMoneyNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when all attributes are set to an empty string
     * <li><b>Parameters:</b></li> a payment object with all attributes set to an empty string
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyEmptyStringAttributes() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"\"," +
                        " \"orderId\":\"\"," +
                        " \"userId\":\"\"," +
                        " \"price\":\"\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is("")))
                .andExpect(jsonPath("$.data.money", is("")));

        // Make sure the money object was saved in the repository
        assertEquals(1, Iterables.size(addMoneyRepository.findAll()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ids contain invalid characters for a UUID
     * <li><b>Parameters:</b></li> a payment object with ids that contain invalid characters for a UUID
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository. The ids are strings without any restrictions, so they are saved as they are.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyInvalidIds() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"this is an id()/\"," +
                        " \"orderId\":\"also and id13&\"," +
                        " \"userId\":\"as well\"," +
                        " \"price\":\")(/&!/§$!\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is("as well")))
                .andExpect(jsonPath("$.data.money", is(")(/&!/§$!")));

        // Make sure the money object was saved in the repository
        assertEquals(1, Iterables.size(addMoneyRepository.findAll()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the payment object has null attributes
     * <li><b>Parameters:</b></li> a payment object with all attributes set to null
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyNullAttributes() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":null," +
                        " \"orderId\":null," +
                        " \"userId\":null," +
                        " \"price\":null}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is(nullValue())))
                .andExpect(jsonPath("$.data.money", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertEquals(1, Iterables.size(addMoneyRepository.findAll()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the payment is already in the repository
     * <li><b>Parameters:</b></li> a payment object with the same orderId as an object in the repository
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository. There can be multiple money objects with the same orderId.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyAlreadyInRepository() throws Exception {
        // Arrange
        Money money = new Money();
        money.setUserId("1");
        money.setMoney("1");
        addMoneyRepository.save(money);

        String requestJson =
                "{\"id\":\"1234567890\"," +
                        " \"orderId\":\"1\"," +
                        " \"userId\":\"1\"," +
                        " \"price\":\"1\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is("1")))
                .andExpect(jsonPath("$.data.money", is("1")));

        // Make sure the money object was not saved in the repository
        assertEquals(2, Iterables.size(addMoneyRepository.findAll()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the JSON does not have any attributes sets
     * <li><b>Parameters:</b></li> empty JSON object
     * <li><b>Expected result:</b></li> status 1, msg "Add Money Success", a configured money object. The money should
     * be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddMoneyEmptyJson() throws Exception {
        // Arrange
        String requestJson = "{}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment/money")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Add Money Success")))
                .andExpect(jsonPath("$.data.userId", is("")))
                .andExpect(jsonPath("$.data.money", is("")));

        // Make sure the payment was saved in the repository
        assertEquals(1, Iterables.size(addMoneyRepository.findAll()));
    }
}
