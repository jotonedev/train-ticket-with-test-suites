package plan.component.post;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import plan.entity.*;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
public class SearchCheapestResultTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel services successfully return a valid list of TripResponses
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A RoutePlanInfo object with valid Stations
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured list of RoutePlanResultUnit
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchCheapestResultCorrectObject() throws Exception {
        // Arrange
        TripId id = new TripId("T");
        ArrayList<TripResponse> tripResponses = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            TripResponse trip = new TripResponse();
            trip.setTripId(id);
            trip.setPriceForConfortClass(String.valueOf(i + 500));
            trip.setPriceForEconomyClass(String.valueOf(i + 200));
            tripResponses.add(trip);
        }

        Response<ArrayList<TripResponse>> mockResponse = new Response<>(1, "Success", tripResponses);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));



        ArrayList<TripResponse> tripResponsesOther = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            TripResponse trip = new TripResponse();
            trip.setTripId(id);
            trip.setPriceForConfortClass(String.valueOf(i + 300));
            trip.setPriceForEconomyClass(String.valueOf(i + 100));
            tripResponsesOther.add(trip);
        }

        mockResponse = new Response<>(1, "Success", tripResponsesOther);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        Route route = new Route();
        route.setStations(new ArrayList<>());
        Response<Route> mockResponse2 = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/routes/" + id).build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        JSONArray jsonArray = (JSONArray)(JSONObject.parseObject(result, Response.class).getData());
        for(int i = 0; i < jsonArray.size(); i++) {
            RoutePlanResultUnit element = jsonArray.getObject(i, RoutePlanResultUnit.class);
            assertEquals(String.valueOf(i + 100), element.getPriceForSecondClassSeat());
        }
        assertEquals(new Response<>(1, "Success", jsonArray), JSONObject.parseObject(result, Response.class));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel services find no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A RoutePlanInfo object with null as start and endstation
     * <li><b>Expected result:</b></li> status 1, msg "Success", and empty list of RoutePlanResultUnits
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchCheapestResultNoTrips() throws Exception {
        // Arrange
        Response<ArrayList<TripResponse>> mockResponse = new Response<>(0, "No content", new ArrayList<>());
        URI uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));


        URI uri2 = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/left").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri2))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = dateFormat.parse("20000301");
        RoutePlanInfo routePlanInfo = new RoutePlanInfo(null, null, date, Integer.MIN_VALUE);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two RoutePlanInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchCheapestResultMultipleObjects() throws Exception {
        // Arrange
        String requestJson =
                "[{\"formStationName\":\"StationA\"," +
                " \"toStationName\":\"StationB\"," +
                " \"Date\":\""+ new Date() +"\"," +
                " \"num\":\"0\"}," +

                " {formStationName\":\"StationA\"," +
                " \"toStationName\":\"StationB\"," +
                " \"Date\":\""+ new Date() +"\"," +
                " \"num\":\"0\"}]";

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
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
    public void testSearchCheapestResultMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
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
    public void testSearchCheapestResultEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
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
    public void testSearchCheapestResultNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }
}
