package plan.component.post;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import plan.entity.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
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
public class SearchMinStopStationsTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the mocked services successfully return a valid responses
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A RoutePlanInfo object with valid Stations
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured list of RoutePlanResultUnit
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchMinStopStationsCorrectObject() throws Exception {
        // Arrange
        Response<String> mockResponseId = new Response<>(1, "Success", "1");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));

        mockResponseId = new Response<>(1, "Success", "2");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        ArrayList<Route> routes = new ArrayList<>();
        for(int i = 0; i < 6; i++) {
            Route route = new Route();
            route.setStations(Arrays.asList("1", "4", "5", "2"));
            routes.add(route);
        }
        Response<ArrayList<Route>> mockResponseRoute = new Response<>(1, "Success", routes);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1/2").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));


        ArrayList<ArrayList<Trip>> trains = new ArrayList<>();
        Trip trip = new Trip();
        trip.setTripId(new TripId("T"));
        trip.setRouteId("1");
        trains.add(new ArrayList<>());
        trains.get(0).add(trip);
        trains.get(0).add(trip);
        Response<ArrayList<ArrayList<Trip>>> mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));

        mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));


        TripAllDetail tripAllDetail = new TripAllDetail();
        tripAllDetail.setTripResponse(new TripResponse());
        Response<TripAllDetail> mockResponseDetail = new Response<>(1, "Success", tripAllDetail);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseDetail), MediaType.APPLICATION_JSON));

        Route route = new Route();
        route.setStations(Arrays.asList("1", "4", "5", "2"));
        Response<Route> mockResponse2 = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponse2), MediaType.APPLICATION_JSON));


        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        String result = mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockServer.verify();
        JSONArray jsonArray = (JSONArray)(JSONObject.parseObject(result, Response.class).getData());
        for(int i = 0; i<jsonArray.size(); i++) {
            RoutePlanResultUnit element = jsonArray.getObject(i, RoutePlanResultUnit.class);
            assertArrayEquals(new String[]{"1", "4", "5", "2"}, element.getStopStations().toArray());
        }
        assertEquals(new Response<>(1, "Success.", jsonArray), JSONObject.parseObject(result, Response.class));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the station and route services find no countent
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A RoutePlanInfo object with null as start and endstation
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The result of the ts-route-service is not null-checked,
     * leading to a NullPointerException when attempting to call size() on the returned object. The issue with the
     * fallback response is that its message to the user is meaningless. Therefore, it is unknown whether the developer
     * knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchMinStopStationsRouteServiceFailure() throws Exception {
        // Arrange
        Response<String> mockResponseId = new Response<>(0, "Not exists", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        mockResponseId = new Response<>(0, "Not exists", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        Response<ArrayList<Route>> mockResponseRoute = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/null/null").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", new Date(), 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two RoutePlanInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchMinStopStationsMultipleObjects() throws Exception {
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
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
    public void testSearchMinStopStationsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
    public void testSearchMinStopStationsEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
    public void testSearchMinStopStationsNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel services fail
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A RoutePlanInfo object with valid Stations
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The result of the ts-travel-service is not null-checked,
     * leading to a NullPointerException when attempting to call size() or get() on the returned object.
     * The issue with the fallback response is that its message to the user is meaningless. Therefore, it is
     * unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testSearchMinStopStationsTravelServiceFailure() throws Exception {
        // Arrange
        Response<String> mockResponseId = new Response<>(1, "Success", "1");
        URI uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationA").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));

        mockResponseId = new Response<>(1, "Success", "2");
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/StationB").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseId), MediaType.APPLICATION_JSON));


        ArrayList<Route> routes = new ArrayList<>();
        for(int i = 0; i < 6; i++) {
            Route route = new Route();
            route.setStations(Arrays.asList("1", "4", "5", "2"));
            routes.add(route);
        }
        Response<ArrayList<Route>> mockResponseRoute = new Response<>(1, "Success", routes);
        uri = UriComponentsBuilder.fromUriString("http://ts-route-service:11178/api/v1/routeservice/routes/1/2").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseRoute), MediaType.APPLICATION_JSON));


        ArrayList<ArrayList<Trip>> trains = new ArrayList<>();
        Trip trip = new Trip();
        trip.setTripId(new TripId("T"));
        trip.setRouteId("1");
        trains.add(new ArrayList<>());
        trains.get(0).add(trip);
        trains.get(0).add(trip);
        Response<ArrayList<ArrayList<Trip>>> mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));

        mockResponseTrainInfo = new Response<>(1, "Success", trains);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trips/routes").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseTrainInfo), MediaType.APPLICATION_JSON));


        Response<TripAllDetail> mockResponseDetail = new Response<>(0, "Error", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel2-service:16346/api/v1/travel2service/trip_detail").build().toUri();

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(mockResponseDetail), MediaType.APPLICATION_JSON));

        // Actual request to the endpoint we want to test
        RoutePlanInfo routePlanInfo = new RoutePlanInfo("StationA", "StationB", null, 1);
        String requestJson = JSONObject.toJSONString(routePlanInfo);

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));

        mockServer.verify();
    }
}
