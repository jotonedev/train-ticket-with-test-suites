package order.component.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import order.entity.Order;
import order.entity.SoldTicket;
import order.repository.OrderRepository;
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

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryAlreadySoldOrdersTest {

    @Autowired
    private OrderRepository orderRepository;

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
        orderRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves for a travelDate and trainNumber that match an order in the database
     * <li><b>Parameters:</b></li> A travelDate and trainNumber that match an order in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured SoldTicket object
     * <li><b>Related Issue:</b></li> <b>F9a:</b> Endpoints of this service take a Date object as a parameter in paths.
     * However, since the arguments of an endpoint are initially set as a String, the argument cannot be transformed to
     * a Date object without using an annotation like @DateTimeFormat to declare the field as convertable to an argument
     * of type Date. Therefore, requesting the endpoint with a Date object as a parameter will always result in a Bad Request.
     * The PathVariable of type Date has been replaced with a String in the endpoint signature, which is then converted to
     * a Date object in the service implementation. This change was done as all test cases requesting this endpoint
     * would fail otherwise. That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testQueryAlreadySoldOrdersMatchingOrder() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        orderRepository.save(order);
        SoldTicket ticket = new SoldTicket();
        ticket.setTravelDate(order.getTravelDate());
        ticket.setTrainNumber(order.getTrainNumber());
        ticket.setFirstClassSeat(1);
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", order.getTravelDate().toString(), order.getTrainNumber())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<SoldTicket> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, SoldTicket.class));
        Assertions.assertEquals(new Response<>(1, "Success", ticket), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no order associated with the given travel data and train number
     * <li><b>Parameters:</b></li> A travelDate and trainNumber that match no order in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured SoldTicket object
     * <li><b>Related Issue:</b></li> <b>F9a:</b> Endpoints of this service take a Date object as a parameter in paths.
     * However, since the arguments of an endpoint are initially set as a String, the argument cannot be transformed to
     * a Date object without using an annotation like @DateTimeFormat to declare the field as convertable to an argument
     * of type Date. Therefore, requesting the endpoint with a Date object as a parameter will always result in a Bad Request.
     * The PathVariable of type Date has been replaced with a String in the endpoint signature, which is then converted to
     * a Date object in the service implementation. This change was done as all test cases requesting this endpoint
     * would fail otherwise. That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testQueryAlreadySoldOrdersNoMatchingOrder() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        SoldTicket ticket = new SoldTicket();
        ticket.setTravelDate(order.getTravelDate());
        ticket.setTrainNumber(order.getTrainNumber());
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", order.getTravelDate().toString(), order.getTrainNumber())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<SoldTicket> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, SoldTicket.class));
        Assertions.assertEquals(new Response<>(1, "Success", ticket), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one travelDate and trainNumber as the URL parameter
     * <li><b>Parameters:</b></li> a travelDate and trainNumber that match an order in the database, and two additional random data
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured SoldTicket object. Only the first needed values are used.
     * <li><b>Related Issue:</b></li> <b>F9a:</b> Endpoints of this service take a Date object as a parameter in paths.
     * However, since the arguments of an endpoint are initially set as a String, the argument cannot be transformed to
     * a Date object without using an annotation like @DateTimeFormat to declare the field as convertable to an argument
     * of type Date. Therefore, requesting the endpoint with a Date object as a parameter will always result in a Bad Request.
     * The PathVariable of type Date has been replaced with a String in the endpoint signature, which is then converted to
     * a Date object in the service implementation. This change was done as all test cases requesting this endpoint
     * would fail otherwise. That is also why the test case contains the prefix "FIXED_PREVIOUSLY_FAILING_".
     * </ul>
     * @throws Exception
     */
    @Test
    public void FIXED_PREVIOUSLY_FAILING_testQueryAlreadySoldOrdersMultipleData() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        orderRepository.save(order);
        SoldTicket ticket = new SoldTicket();
        ticket.setTravelDate(order.getTravelDate());
        ticket.setTrainNumber(order.getTrainNumber());
        ticket.setFirstClassSeat(1);
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", order.getTravelDate().toString(), order.getTrainNumber(), new Date(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<SoldTicket> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, SoldTicket.class));
        Assertions.assertEquals(new Response<>(1, "Success", ticket), response);
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
    public void testQueryAlreadySoldOrdersNonExistingTrainNumber() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", new Date().toString()));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testQueryAlreadySoldOrdersMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/security/{checkDate}/{accountId}", new Date(), UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
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
