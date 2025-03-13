package inside_payment.component.get;

import inside_payment.entity.Money;
import inside_payment.repository.AddMoneyRepository;
import inside_payment.repository.PaymentRepository;
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
public class DrawbackTest {

    @Autowired
    protected AddMoneyRepository addMoneyRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

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
        addMoneyRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves for a valid for when the money exists in the database
     * <li><b>Parameters:</b></li> a money object that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Draw Back Money Success", a null data object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDrawbackMatchingId() throws Exception {
        // Arrange
        Money money = createSampleMoney();
        addMoneyRepository.save(money);

        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}", money.getId(), money.getMoney())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Draw Back Money Success")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Draw Back Money Success", a null data object. Only the first id is used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDrawbackMultipleId() throws Exception {
        // Arrange
        Money money = createSampleMoney();
        addMoneyRepository.save(money);

        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}", money.getId(), money.getMoney(), UUID.randomUUID().toString(), "1")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                .andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Draw Back Money Success")))
                .andExpect(jsonPath("$.data", nullValue()));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id
     * <li><b>Parameters:</b></li> some random id that does not match any element in the database
     * <li><b>Expected result:</b></li> status 1, msg "Draw Back Money Failed", no data
     * <li><b>Related Issue:</b></li> <b>F4b:</b> The return value for {@code addMoneyRepository.findByUserId(userId)}
     * should check if the list is empty. Currently, it only checks if the list is not null, which will always result in a successful response.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDrawbackNoMatchingId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}", UUID.randomUUID().toString(), "1")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Draw Back Money Failed")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    public void testDrawbackNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDrawbackMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}",UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString(), '1')
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private Money createSampleMoney() {
        Money money = new Money();
        money.setUserId("123");
        money.setMoney("200.0");
        return money;
    }
}
