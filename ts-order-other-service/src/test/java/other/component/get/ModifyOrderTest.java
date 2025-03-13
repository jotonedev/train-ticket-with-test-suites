package other.component.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Assertions;
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
import other.entity.Order;
import other.entity.OrderStatus;
import other.repository.OrderOtherRepository;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ModifyOrderTest {

    @Autowired
    private OrderOtherRepository orderOtherRepository;

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
        orderOtherRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves for a valid id with orders that exists in the database
     * <li><b>Parameters:</b></li> an order object with an id that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Modify Order Success", the order object with updated status
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyOrderMatchingId() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        orderOtherRepository.save(order);
        order.setStatus(OrderStatus.PAID.getCode());
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", order.getId().toString(), OrderStatus.PAID.getCode())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<Order> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
        Assertions.assertEquals(new Response<>(1, "Success", order), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves for a valid id with orders that does not exist in the database
     * <li><b>Parameters:</b></li> an order object with an id that does not exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyOrderNotMatchingId() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", order.getId().toString(), OrderStatus.PAID.getCode())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<Order> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
        Assertions.assertEquals(new Response<>(0, "Order Not Found",null), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the id given in paths and additional random ids
     * <li><b>Expected result:</b></li> status 1, msg "Modify Order Success", the order object with updated status. Only the first id in paths is used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyOrderMultipleId() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        orderOtherRepository.save(order);
        order.setStatus(OrderStatus.PAID.getCode());
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", order.getId().toString(), OrderStatus.PAID.getCode(), UUID.randomUUID().toString(), OrderStatus.PAID.getCode())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<Order> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, Order.class));
        Assertions.assertEquals(new Response<>(1, "Success", order), response);
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
    public void testModifyOrderNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", OrderStatus.PAID.getCode()));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give an id, which is not the correct format for an id.
     * <li><b>Parameters:</b></li> an id that does not follow the UUID format
     * <li><b>Expected result:</b></li> status 0, some message indicating that the id is not in the correct format, no data.
     * <li><b>Related Issue:</b></li> <b>F27e:</b> The implementation of the service does not check whether the id is in
     * the correct UUID format before trying to convert it. Therefore, the service throws an exception when the id is not
     * in the correct format.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testModifyOrderIncorrectFormatId() throws Exception {
        String uuid = "not a correct format id";

        // Act
        mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", uuid, OrderStatus.PAID.getCode())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                // We do not care for the exact message, as long as the response is not successful for the invalid UUID format
                .andExpect(jsonPath("$.msg", any(String.class)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testModifyOrderMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/orderOtherService/orderOther/status/{orderId}/{status}", UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString(), OrderStatus.PAID.getCode())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private Order createSampleOrder() {
        Order order = new Order();
        order.setId(UUID.fromString("5ad7750b-a68b-49c0-a8c0-32776b067703"));
        order.setBoughtDate(new Date());
        order.setTravelDate(new Date("Sat Jul 29 00:00:00 GMT+0800 2017"));
        order.setTravelTime(new Date("Mon May 04 09:02:00 GMT+0800 2013"));
        order.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        order.setContactsName("contactName");
        order.setDocumentType(1);
        order.setContactsDocumentNumber("contactDocumentNumber");
        order.setTrainNumber("G1237");
        order.setCoachNumber(5);
        order.setSeatClass(2);
        order.setSeatNumber("1");
        order.setFrom("nanjing");
        order.setTo("shanghaihongqiao");
        order.setStatus(0);
        order.setPrice("100.0");
        return order;
    }
}
