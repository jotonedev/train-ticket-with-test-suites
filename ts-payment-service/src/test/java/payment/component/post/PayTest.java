package payment.component.post;

import com.trainticket.PaymentApplication;
import com.trainticket.entity.Payment;
import com.trainticket.repository.PaymentRepository;
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
public class PayTest {

    @Autowired
    private PaymentRepository paymentRepository;

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
        paymentRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give it a correct object
     * <li><b>Parameters:</b></li> a payment object with valid values for all attributes
     * <li><b>Expected result:</b></li> status 1, msg "Pay Success", no data. The payment should be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayCorrectObject() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"orderId\":\"1\"," +
                " \"userId\":\"1\"," +
                " \"price\":\"1\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertNotNull(paymentRepository.findByOrderId("1"));
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
    public void testPayMultipleObjects() throws Exception {
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
        mockMvc.perform(post("/api/v1/paymentservice/payment")
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
    public void testPayMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
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
    public void testPayMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
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
    public void testPayNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/paymentservice/payment")
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
     * <li><b>Expected result:</b></li> status 1, msg "Pay Success", no data. The payment should be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayEmptyStringAttributes() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"\"," +
                " \"orderId\":\"\"," +
                " \"userId\":\"\"," +
                " \"price\":\"\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertNotNull(paymentRepository.findByOrderId(""));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the ids contain invalid characters for a UUID
     * <li><b>Parameters:</b></li> a payment object with ids that contain invalid characters for a UUID
     * <li><b>Expected result:</b></li> status 1, msg "Pay Success", no data. The payment should be saved in the repository.
     * The ids are strings without any restrictions, so they are valid.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayInvalidIds() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":\"this is an id()/\"," +
                " \"orderId\":\"also and id13&\"," +
                " \"userId\":\"as well\"," +
                " \"price\":\")(/&!/§$!\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertNotNull(paymentRepository.findByOrderId("also and id13&"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the payment object has null attributes
     * <li><b>Parameters:</b></li> a payment object with all attributes set to null
     * <li><b>Expected result:</b></li> status 1, msg "Pay Success", no data. The payment should be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayNullAttributes() throws Exception {
        // Arrange
        String requestJson =
                "{\"id\":null," +
                " \"orderId\":null," +
                " \"userId\":null," +
                " \"price\":null}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertEquals(1, paymentRepository.findAll().size());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the payment is already in the repository
     * <li><b>Parameters:</b></li> a payment object with the same orderId as an object in the repository
     * <li><b>Expected result:</b></li> status 0, msg "Pay Failed, order not found with order" + id, no data.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayAlreadyInRepository() throws Exception {
        // Arrange
        Payment payment = new Payment();
        payment.setOrderId("1");
        paymentRepository.save(payment);

        String requestJson =
                "{\"id\":\"1234567890\"," +
                " \"orderId\":\"1\"," +
                " \"userId\":\"1\"," +
                " \"price\":\"1\"}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Pay Failed, order not found with order id1")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the JSON does not have any attributes sets
     * <li><b>Parameters:</b></li> empty JSON object
     * <li><b>Expected result:</b></li> status 1, msg "Pay Success", no data. The payment should be saved in the repository.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPayEmptyJson() throws Exception {
        // Arrange
        String requestJson = "{}";

        // Act
        mockMvc.perform(post("/api/v1/paymentservice/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Pay Success")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        // Make sure the payment was saved in the repository
        assertEquals(1, paymentRepository.findAll().size());
    }
}
