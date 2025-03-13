package security.component.get;

import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import security.entity.OrderSecurity;
import security.entity.SecurityConfig;
import security.repository.SecurityRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CheckAccountIdTest {

    @Autowired
    private SecurityRepository securityRepository;

    @MockBean
    private RestTemplate restTemplate;

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
        securityRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the sum of both responses is lower than initialized value in both
     * orderInOneHour and validOrder
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> any UUID as accountId
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCheckCorrectId() throws Exception {
        // Arrange
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumInLastOneHour(10);
        responseData.setOrderNumOfValidOrder(15);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        UUID accountId = UUID.randomUUID();
        initData(accountId);

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", accountId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(accountId.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> any multiple UUIDs as accountId
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id. Only the first id in paths is used
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCheckMultipleIds() throws Exception {
        // Arrange
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumInLastOneHour(10);
        responseData.setOrderNumOfValidOrder(15);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        UUID accountId = UUID.randomUUID();
        initData(accountId);

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", accountId, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(accountId.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCheckMalformedId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testCheckMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when id cannot be found by the called services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> any UUID as accountId
     * <li><b>Expected result:</b></li> status 1, msg "Success.r", the provided id. Since the called services cannot find
     * any orders with the provided id, the services return its default values (0)
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCheckNonExistingId() throws Exception {
        // Arrange
        OrderSecurity responseData = new OrderSecurity();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        initData(UUID.randomUUID());
        UUID notExistingId = UUID.randomUUID();

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", notExistingId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.r")))
                .andExpect(jsonPath("$.data", is(notExistingId.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the sum of both responses is higher than initialized value in both
     * orderInOneHour and validOrder
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> any UUID as accountId
     * <li><b>Expected result:</b></li> status 0, msg "Too much order in last one hour or too much valid order", the provided id.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCheckValuesTooHigh() throws Exception {
        // Arrange
        OrderSecurity responseData = new OrderSecurity();
        responseData.setOrderNumInLastOneHour(20);
        responseData.setOrderNumOfValidOrder(20);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/security/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", responseData), HttpStatus.OK));

        UUID accountId = UUID.randomUUID();
        initData(accountId);

        // Act
        mockMvc.perform(get("/api/v1/securityservice/securityConfigs/{accountId}", accountId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Too much order in last one hour or too much valid order")))
                .andExpect(jsonPath("$.data", is(accountId.toString())));
    }

    private void initData(UUID id) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setId(id);
        securityConfig.setValue("20");
        securityConfig.setName("max_order_1_hour");
        securityRepository.save(securityConfig);
        id = UUID.randomUUID();
        securityConfig.setId(id);
        securityConfig.setValue("30");
        securityConfig.setName("max_order_not_use");
        securityRepository.save(securityConfig);
    }
}
