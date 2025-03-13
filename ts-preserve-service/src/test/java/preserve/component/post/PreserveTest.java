package preserve.component.post;

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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import preserve.entity.*;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

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

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class PreserveTest {

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
     *   <li>ts-order-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-security-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-basic-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   <li>ts-consign-price-service</li>
     *   <li>ts-config-service</li>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object
     * <li><b>Expected result:</b></li> status 1, msg "Success.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveCorrectObject() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Order order = new Order();
        order.setId(id);
        order.setAccountId(id);
        Response<Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/1/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is("Success")));

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
    public void testPreserveMultipleObjects() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String requestJson =
                "[{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}," +

                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}]";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
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
    public void testPreserveMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
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
    public void testPreserveMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
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
    public void testPreserveNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the accountId has the wrong format
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with an accountId of "INVALID"
     * <li><b>Expected result:</b></li> status 0, msg "Error.", no data. Message of Security Service is forwarded.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveAccountIdInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(0, "Error", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/wrongformat").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));


        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"wrongformat\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2, \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Error")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the contactsId has the wrong format
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with an contactsId of "wrongFormat"
     * <li><b>Expected result:</b></li> status 0, msg "Error.", no data. Message of Contacts Service is forwarded.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveContactsIdInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Response<Contacts> mockResponse2 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/wrongFormat").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"wrongFormat\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Error")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId has the wrong format
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with an tripId of "INVALID"
     * <li><b>Expected result:</b></li> status 0, msg "Error.", no data. Message of Travel Service is forwarded.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveTripIdWrongFormat() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        Response<TripAllDetail> mockResponse3 = new Response<>(0, "Error", new TripAllDetail());
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"INVALID\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Error")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the seatType is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-assurance-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-consign-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a seatType of Integer.MIN_VALUE
     * <li><b>Expected result:</b></li> status 1, msg "Success.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveSeattypeInvalid() throws Exception {
        // Arrange
        //Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Order order = new Order();
        order.setId(id);
        order.setAccountId(id);
        Response<Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/1/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));

        //Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":" + Integer.MIN_VALUE + "," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is("Success")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are too few seats
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a seatType of 3
     * <li><b>Expected result:</b></li> status 0, msg "Seat Not Enough", no data
     * <li><b>Related Issue:</b></li> <b>F25b</b> Seattype is compared with the number of available seats, which does not make
     * sense logically and leads to errors, although "Seat Not Enough" should be returned.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPreserveSeattypeTooFewSeats() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setEconomyClass(0);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));


        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":3," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the date is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a date that is not after today
     * <li><b>Expected result:</b></li> status 0, msg "Error", no data. Message of Order Service is forwarded.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveDateInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Response<Order> mockResponse7 = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));


        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2000-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":1," +
                " \"foodType\":2," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"name\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Error")))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when from and to station names are invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with invalid station names for from and to
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveFromToInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/1").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        Response<TravelResult> mockResponse5 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Response<Ticket> mockResponse6 = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\", \"contactsId\":\"" + id + "\", \"tripId\":\"" + id + "\", \"seatType\":2, \"date\":\"2024-01-01\", \"from\":\"1\", \"to\":\"1\", \"assurance\":1, \"foodType\":2, \"stationName\":\"station\", \"storeName\":\"store\", \"foodName\":\"food\", \"foodPrice\":5.99, \"handleDate\":\"date\", \"consigneeName\":\"name\", \"consigneePhone\":\"911\", \"consigneeWeight\":75.82, \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the assurance type is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-assurance-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with an assurance type != 0
     * <li><b>Expected result:</b></li> status 1, msg "Success.But Buy Assurance Fail.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveAssuranceInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Order order = new Order();
        order.setId(id);
        order.setAccountId(id);
        Response<Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response mockAssurance = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-assurance-service:18888/api/v1/assuranceservice/assurances/"+ Integer.MIN_VALUE + "/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAssurance), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":"+ Integer.MIN_VALUE + "," +
                " \"foodType\":0," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.But Buy Assurance Fail.")))
                .andExpect(jsonPath("$.data", is("Success")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the food type is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notificatioin-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with a foodType of INTEGER.MIN_VALUE
     * <li><b>Expected result:</b></li> status 1, msg "Success.But Buy Food Fail.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveFoodtypeInvalid() throws Exception {
        // Arrange
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Order order = new Order();
        order.setId(id);
        order.setAccountId(id);
        Response<Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response mockFood = new Response(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-food-service:18856/api/v1/foodservice/orders").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockFood), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson = "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":0," +
                " \"foodType\":" + Integer.MIN_VALUE + "," +
                " \"stationName\":\"1\"," +
                " \"storeName\":\"1\"," +
                " \"foodName\":\"1\"," +
                " \"foodPrice\":-3.99," +
                " \"handleDate\":\"date\"," +
                " \"consigneeName\":\"\"," +
                " \"consigneePhone\":\"911\"," +
                " \"consigneeWeight\":75.82," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.But Buy Food Fail.")))
                .andExpect(jsonPath("$.data", is("Success")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the consignee is invalid
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-security-service</li>
     *   <li>ts-contacts-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-food-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notificatioin-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a valid request object with an invalid consignee name
     * <li><b>Expected result:</b></li> status 1, msg "Consign Fail.", data "Success"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveInvalidConsigneeInfo() throws Exception {
        // Assert
        // Mock responses of external services for every request this service does for the endpoint
        UUID id = UUID.randomUUID();

        Response mockResponse1 = new Response<>(1, "Success", "not relevant");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-security-service:11188/api/v1/securityservice/securityConfigs/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse1), MediaType.APPLICATION_JSON));

        Contacts contacts = new Contacts();
        Response<Contacts> mockResponse2 = new Response<>(1, "Success", contacts);
        uri = UriComponentsBuilder.fromUriString("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        tripAllDetail.setTrip(new Trip());
        tripAllDetail.getTripResponse().setConfortClass(3);
        tripAllDetail.getTripResponse().setStartingTime(new Date());
        Response<TripAllDetail> mockResponse3 = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trip_detail").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse3), MediaType.APPLICATION_JSON));

        Response<String> mockResponse4 = new Response<>(1, "Success", "1");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/stationA").build().toUri();
        mockServer.expect(ExpectedCount.twice(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse4), MediaType.APPLICATION_JSON));

        TravelResult travelResult = new TravelResult();
        travelResult.setPrices(new HashMap<>());
        travelResult.getPrices().put("confortClass", "100");
        Response<TravelResult> mockResponse5 = new Response<>(1, "Success", travelResult);
        uri = UriComponentsBuilder.fromUriString("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse5), MediaType.APPLICATION_JSON));

        Ticket ticket = new Ticket();
        Response<Ticket> mockResponse6 = new Response<>(1, "Success", ticket);
        uri = UriComponentsBuilder.fromUriString("http://ts-seat-service:18898/api/v1/seatservice/seats").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse6), MediaType.APPLICATION_JSON));

        Order order = new Order();
        order.setId(id);
        order.setAccountId(id);
        Response<Order> mockResponse7 = new Response<>(1, "Success", order);
        uri = UriComponentsBuilder.fromUriString("http://ts-order-service:12031/api/v1/orderservice/order").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse7), MediaType.APPLICATION_JSON));

        Response mockConsign = new Response(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-consign-service:16111/api/v1/consignservice/consigns").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockConsign), MediaType.APPLICATION_JSON));

        User user = new User();
        Response<User> mockResponse8 = new Response<>(1, "Success", user);
        uri = UriComponentsBuilder.fromUriString("http://ts-user-service:12342/api/v1/userservice/users/id/" + id).build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse8), MediaType.APPLICATION_JSON));

        uri = UriComponentsBuilder.fromUriString("http://ts-notification-service:17853/api/v1/notifyservice/notification/preserve_success").build().toUri();
        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(true), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        String requestJson =
                "{\"accountId\":\"" + id + "\"," +
                " \"contactsId\":\"" + id + "\"," +
                " \"tripId\":\"" + id + "\"," +
                " \"seatType\":2," +
                " \"date\":\"2025-01-01\"," +
                " \"from\":\"stationA\"," +
                " \"to\":\"stationA\"," +
                " \"assurance\":0, \"foodType\":0," +
                " \"stationName\":\"station\"," +
                " \"storeName\":\"store\"," +
                " \"foodName\":\"food\"," +
                " \"foodPrice\":5.99," +
                " \"handleDate\":\"noDate\"," +
                " \"consigneeName\":\"()42397)\"," +
                " \"consigneePhone\":\"noNumber\"," +
                " \"consigneeWeight\":-147.14," +
                " \"isWithin\":true}";

        // Act
        mockMvc.perform(post("/api/v1/preserveservice/preserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Consign Fail.")))
                .andExpect(jsonPath("$.data", is("Success")));

        mockServer.verify();
    }
}
