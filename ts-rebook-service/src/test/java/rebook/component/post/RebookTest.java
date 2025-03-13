package rebook.component.post;

import com.alibaba.fastjson.JSONObject;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import rebook.entity.*;

import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
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
public class RebookTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request object is correct
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookCorrectObject() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse5 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response mockResponse6 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.documentType", is(0)))
                .andExpect(jsonPath("$.data.seatNumber", is("0")))
                .andExpect(jsonPath("$.data.differenceMoney", is("0.0")))
                .andExpect(jsonPath("$.data.seatClass", is(2)))
                .andExpect(jsonPath("$.data.price", is("180")))
                .andExpect(jsonPath("$.data.from", is("1")))
                .andExpect(jsonPath("$.data.id", is(id.toString())))
                .andExpect(jsonPath("$.data.trainNumber", is("G")))
                .andExpect(jsonPath("$.data.to", is("1")))
                .andExpect(jsonPath("$.data.status", is(3)));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order status returned by the order service is "CHANGE"
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object
     * <li><b>Related Issue:</b></li> <b>F23:</b> more specific if-statements come after general if-statements, making the
     * specific statements like {@code else if (status == OrderStatus.CHANGE.getCode())} or {@code else if (status == OrderStatus.COLLECTED.getCode())}
     * unreachable.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testRebookOrderStatusChange() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.CHANGE.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));


        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("You have already changed your ticket and you can only change one time.")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order status returned by the order service is "COLLECTED"
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a configured order object
     * <li><b>Related Issue:</b></li> <b>F23:</b> more specific if-statements come after general if-statements, making the
     * specific statements like {@code else if (status == OrderStatus.CHANGE.getCode())} or {@code else if (status == OrderStatus.COLLECTED.getCode())}
     * unreachable.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testRebookOrderStatusCollected() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.COLLECTED.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));


        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("You have already collected your ticket and you can change it now.")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Station objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookMultipleObjects() throws Exception {
        // Arrange
        String requestJson =
                "[{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}," +
                        "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}]";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
    public void testRebookMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
    public void testRebookMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
    public void testRebookNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the loginId does not exist for the inside payment service
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a random UUID as loginId
     * <li><b>Expected result:</b></li> status 0, msg "Can't draw back the difference money, please try again!", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookLoginIdNonExisting() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(0, "Not existing", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Can't draw back the difference money, please try again!")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the orderId does not exist for the order service
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a random UUID as orderId
     * <li><b>Expected result:</b></li> status 0, msg "order not found", no data.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookOrderIdNonExisting() throws Exception {
        // Arrange
        Response<Order> mockResponse1 = new Response<>(0, "Not exists", new Order());
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("order not found")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId does not exist for the travel service
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object where the tripId does not exist
     * <li><b>Expected result:</b></li> status 0, msg "not exists", no data. The message is forwarded from the travel service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookTripIdNonExisting() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(0, "Not exists", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Not exists")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when both tripIds are from the same type, which means they both begin with
     * "G" or "D" or both do not.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel2-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object where both tripIds are from the same type
     * <li><b>Expected result:</b></li> status 0, msg "Can't update order!", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookTripIdSameType() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse5 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response mockResponse6 = new Response<>(0, "Error", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        //Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"Z\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Can't update Order!")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is valid but there are too few seats available
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object where there are too few seats available
     * <li><b>Expected result:</b></li> status 0, msg "Seat Not Enough", no data
     * <li><b>Related Issue:</b></li> <b>F25a</b> Seattype is compared with the number of available seats, which does not make
     * sense logically and leads to errors, although "Seat Not Enough" should be returned.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testRebookTooFewSeats() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setEconomyClass(0);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":3," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Seat Not Enough")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is neither 2 nor 3
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with seatType 1
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookSeatTypeInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("200");
        order.setTrainNumber("Z");
        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response mockResponse4 = new Response<>(1, "Success", "not relevant");
        uri = UriComponentsBuilder.fromUriString("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/1/20").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Response<Ticket> mockResponse5 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":1," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /*
     * The last equivalence class we want to test is the case if the new ticket price is higher than the old one. Normally
     * we can achieve this hypothetical, if the new trainType is the more expensive high speed train with the tripId beginning with
     * "G" or "D". As sake for a correct test sequence, we did not do this in the first test, but now we get a new response
     * for this case.
     */
    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the new ticket price is higher than the old one
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object where the new trainType is the more expensive high speed
     * train with the tripId beginning with "G" or "D".
     * <li><b>Expected result:</b></li> status 2, msg "Please pay the different money!", a configured order object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testRebookCantPayDifference() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTravelDate(2026, 1, 1);
        order.setTravelTime(new Date());
        order.setFrom("1");
        order.setTo("1");
        order.setPrice("160");
        order.setTrainNumber("Z");

        Response<Order> mockResponse1 = new Response<>(1, "Success", order);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/1").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response mockResponse2 = new Response<>(1, "Success", "stationName");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/name/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setPriceForConfortClass("180");
        tripAllDetail.setTrip(new Trip());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"loginId\":\"1\"," +
                        " \"orderId\":\"1\"," +
                        " \"oldTripId\":\"1\"," +
                        " \"tripId\":\"G\"," +
                        " \"seatType\":2," +
                        " \"date\":null}";

        // Act
        mockMvc.perform(post("/api/v1/rebookservice/rebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(2)))
                .andExpect(jsonPath("$.msg", is("Please pay the different money!")))
                .andExpect(jsonPath("$.data.differenceMoney", is("20")));

        mockServer.verify();
    }
}
