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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryAddMoneyTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when there are Money objects in the database
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Query Money Success", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryAddMoneyElementsInDatabase() throws Exception {
        // Arrange
        Money money = new Money();
        addMoneyRepository.save(money);

        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/money")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Query Money Success")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryAddMoneyEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/inside_pay_service/inside_payment/money")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("")))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
