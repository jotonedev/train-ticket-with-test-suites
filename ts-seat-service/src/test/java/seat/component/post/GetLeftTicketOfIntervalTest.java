package seat.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import seat.entity.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetLeftTicketOfIntervalTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request object is correct
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", data 3 (number of tickets left)
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetLeftTicketsOfIntervalCorrectObject() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i < 6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i < 5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("2");
            ticket.setSeatNo(new Random().nextInt(10));
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(3);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.0));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"travelDate\":\"2024-12-01\"," +
                " \"trainNumber\":\"G\"," +
                " \"startStation\":\"3\"," +
                " \"destStation\":\"5\"," +
                " \"seatType\":2}";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(3)));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Seat objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetLeftTicketsOfIntervalMultipleObjects() throws Exception {
        // Arrange
        String requestJson =
                "[{\"travelDate\":\"2024-12-01\"," +
                " \"trainNumber\":\"G\"," +
                " \"startStation\":\"1\"," +
                " \"destStation\":\"4\"," +
                " \"seatType\":2}," +

                "{\"travelDate\":\"2024-12-01\"," +
                " \"trainNumber\":\"G\"," +
                " \"startStation\":\"1\"," +
                " \"destStation\":\"4\"," +
                " \"seatType\":2}]";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
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
    public void testGetLeftTicketsOfIntervalMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"travelDate\":\"notADate\", \"trainNumber\":invalid, \"startStation\":invalid, \"destStation\":invalid, \"seatType\":null}";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
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
    public void testGetLeftTicketsOfIntervalEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
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
    public void testCreateNewSecurityConfigNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trainNumber does not start with "G" or "D"
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a trainNumber that does not start with "G" or "D"
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", data 4 (number of tickets left)
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetLeftTicketsOfIntervalNotStartingWithGorD() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i < 6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/routes/Z").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i < 5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("5");
            ticket.setSeatNo(i);
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setConfortClass(50);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/train_types/Z").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.8));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"travelDate\":\"2024-12-01\"," +
                " \"trainNumber\":\"Z\"," +
                " \"startStation\":\"1\"," +
                " \"destStation\":\"4\"," +
                " \"seatType\":2}";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(4)));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trainNumber does not exist in the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a trainNumber that does not exist in the other services
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetLeftTicketsOfIntervalStartStationDestStationNonExisting() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        Response<Route> mockResponse1 = new Response<>(0, "Error", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/Gwrong").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response<LeftTicketInfo> mockResponse2 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        Response<TrainType> mockResponse3 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/Gwrong").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"travelDate\":\"2024-12-01\"," +
                " \"trainNumber\":\"wrong\"," +
                " \"startStation\":\"wrong\"," +
                " \"destStation\":\"wrong\"," +
                " \"seatType\":2}";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is out of range
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a seatType of 0
     * <li><b>Expected result:</b></li> status 1, msg "Get Left Ticket of Internal Success", 2 (number of left tickets). The seatType will be assumed to be 3.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetLeftTicketsOfIntervalSeatTypeOutOfRange() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        Route route = new Route();
        route.setStations(new ArrayList<>());
        for(int i = 1; i<6; i++) {
            route.getStations().add(String.valueOf(i));
        }
        Response<Route> mockResponse1 = new Response<>(1, "Success", route);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
        leftTicketInfo.setSoldTickets(new HashSet<>());
        for(int i = 0; i<5; i++) {
            Ticket ticket = new Ticket();
            ticket.setDestStation("5");
            ticket.setSeatNo(i);
            leftTicketInfo.getSoldTickets().add(ticket);
        }
        Response<LeftTicketInfo> mockResponse2 = new Response<>(1, "Success", leftTicketInfo);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order/tickets").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TrainType trainType = new TrainType();
        trainType.setEconomyClass(10);
        Response<TrainType> mockResponse3 = new Response<>(1, "Success", trainType);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/train_types/G").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Config config = new Config();
        config.setValue(String.valueOf(0.3));
        Response<Config> mockResponse4 = new Response<>(1, "Success", config);
        uri = UriComponentsBuilder.fromUriString("http://ts-config-service:15679/api/v1/configservice/configs/DirectTicketAllocationProportion").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"travelDate\":\"2024-12-01\"," +
                        " \"trainNumber\":\"G\"," +
                        " \"startStation\":\"1\"," +
                        " \"destStation\":\"4\"," +
                        " \"seatType\":0}";

        // Act
        mockMvc.perform(post("/api/v1/seatservice/seats/left_tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get Left Ticket of Internal Success")))
                .andExpect(jsonPath("$.data", is(2)));

        mockServer.verify();
    }
}
