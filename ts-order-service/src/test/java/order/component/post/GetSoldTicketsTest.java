package order.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import order.entity.LeftTicketInfo;
import order.entity.Order;
import order.entity.Seat;
import order.entity.Ticket;
import order.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetSoldTicketsTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass a seat object with valid values for all attributes
     * <li><b>Parameters:</b></li> A seat object with values that match an order in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured LeftTicketInfo object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetSoldTicketsCorrectObject() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        orderRepository.save(order);
        Seat seat = new Seat();
        seat.setTravelDate(order.getTravelDate());
        seat.setTrainNumber(order.getTrainNumber());
        String jsonRequest = new ObjectMapper().writeValueAsString(seat);
        Set ticketSet = new HashSet();
        ticketSet.add(new Ticket(Integer.parseInt(order.getSeatNumber()),
                order.getFrom(), order.getTo()));
        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(ticketSet);
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<LeftTicketInfo> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, LeftTicketInfo.class));
        Assertions.assertEquals(new Response<>(1, "Success", leftTicketInfo), response);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the stored order has a seat number that is not a number
     * <li><b>Parameters:</b></li> A seat object with values that match an order in the database
     * <li><b>Expected result:</b></li> The service should not crash and return an a failing response indicating that
     * no orders were found (Like status 0, msg "Order is Null.", no data)
     * * <li><b>Related Issue:</b></li> <b>F17a:</b> The implementation of the service does not handle the case
     * when orders in the database have a SeatNumber as a String that cannot be parsed to an integer. Therefore,
     * {@code Integer.parseInt(tempOrder.getSeatNumber())} will always lead to an unhandled NumberFormatException.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetSoldTicketsOrderSeatNumberNotANumber() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        order.setSeatNumber("Not a number!");
        orderRepository.save(order);

        Seat seat = new Seat();
        seat.setTravelDate(order.getTravelDate());
        seat.setTrainNumber(order.getTrainNumber());
        String jsonRequest = new ObjectMapper().writeValueAsString(seat);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order is Null.")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Order objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetSoldTicketsMultipleObjects() throws Exception {
        // Arrange
        Seat[] seats = {new Seat(), new Seat()};
        String jsonRequest = new ObjectMapper().writeValueAsString(seats);

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when when there are no orders in the database
     * <li><b>Parameters:</b></li> A seat with valid values for all attributes, but no match with orders in the repository.
     * <li><b>Expected result:</b></li> status 0, msg "Order is Null", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetSoldTicketsMissingObject() throws Exception {
        // Arrange
        Order order = createSampleOrder();
        String jsonRequest = new ObjectMapper().writeValueAsString(order);
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

        // Act
        String result = mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Response<LeftTicketInfo> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, LeftTicketInfo.class));
        Assertions.assertEquals(new Response<>(0, "Order is Null.", null), response);
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
    public void testGetSoldTicketsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
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
    public void testGetSoldTicketsEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
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
    public void testGetSoldTicketsNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
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
