package admintravel.component.get;

import admintravel.entity.AdminTrip;
import admintravel.entity.Trip;
import edu.fudan.common.util.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllTravelsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel and travel2 service return no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No Content", an empty array list
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllTravelsTravelAndTravel2NoContent() throws Exception {
        Response<ArrayList<AdminTrip>> travelResponse = new Response<>(0, "No Content", null);
        Response<ArrayList<AdminTrip>> travel2Response = new Response<>(0, "No Content", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travel2Response, HttpStatus.OK));


        mockMvc.perform(get("/api/v1/admintravelservice/admintravel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel returns a list of adminTrips and travel2 service returns no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", the combined list of admin trips from both services
     * <li><b>Related Issue:</b></li> <b>F3:</b> The implementation of the service calls the travel and travel2 service
     * to combine their responses to a list. If that combined list is not empty, it should be returned with a successful message
     * and a status code of 1. However, the results of the first service are always overwritten by the results of the second
     * service. Therefore, if the second service does not return any content, the endpoint will return a status of 0 and a message
     * of "No Content" regardless of the results of the first service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetAllTravelsTravel2NoContent() throws Exception {
        // Arrange
        ArrayList<AdminTrip> adminTripList = generateRandomAdminTripList(3);
        ArrayList<AdminTrip> adminTrip2List = new ArrayList<>();
        List<String> expectedTotalRouteIdsList = extractRouteIdUUIDListFromTwoAdminTripArrays(adminTripList, adminTrip2List);

        Response<ArrayList<AdminTrip>> travelResponse = new Response<>(1, "Success.", adminTripList);
        Response<ArrayList<AdminTrip>> travel2Response = new Response<>(0, "No content", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travel2Response, HttpStatus.OK));

        // Act
        MvcResult result =  mockMvc.perform(get("/api/v1/admintravelservice/admintravel"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalRouteIdsList.size())))
                .andReturn();

        List<String> responseIds = extractRouteIdUUIDListFromMVCResult(result);
        assertThat(expectedTotalRouteIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel2 returns a list of adminTrips and travel service returns no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", the combined list of admin trips from both services
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllTravelsTravelNoContent() throws Exception {
        // Arrange
        ArrayList<AdminTrip> adminTrip2List = generateRandomAdminTripList(3);
        ArrayList<AdminTrip> adminTripList = new ArrayList<>();
        List<String> expectedTotalRouteIdsList = extractRouteIdUUIDListFromTwoAdminTripArrays(adminTripList, adminTrip2List);

        Response<ArrayList<AdminTrip>> travelResponse = new Response<>(0, "No content", null);
        Response<ArrayList<AdminTrip>> travel2Response = new Response<>(1, "Success", adminTrip2List);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travel2Response, HttpStatus.OK));

        // Act
        MvcResult result =  mockMvc.perform(get("/api/v1/admintravelservice/admintravel"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalRouteIdsList.size())))
                .andReturn();

        List<String> responseIds = extractRouteIdUUIDListFromMVCResult(result);
        assertThat(expectedTotalRouteIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel and travel2 return a list of adminTrips
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", the combined list of admin trips from both services
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllTravelsBothServicesNotEmpty() throws Exception {
        // Arrange
        ArrayList<AdminTrip> adminTrip2List = generateRandomAdminTripList(3);
        ArrayList<AdminTrip> adminTripList = generateRandomAdminTripList(4);
        List<String> expectedTotalRouteIdsList = extractRouteIdUUIDListFromTwoAdminTripArrays(adminTripList, adminTrip2List);

        Response<ArrayList<AdminTrip>> travelResponse = new Response<>(1, "Success", adminTripList);
        Response<ArrayList<AdminTrip>> travel2Response = new Response<>(1, "Success", adminTrip2List);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<AdminTrip>>>>any()))
                .thenReturn(new ResponseEntity<>(travel2Response, HttpStatus.OK));

        // Act
        MvcResult result =  mockMvc.perform(get("/api/v1/admintravelservice/admintravel"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalRouteIdsList.size())))
                .andReturn();

        List<String> responseIds = extractRouteIdUUIDListFromMVCResult(result);
        assertThat(expectedTotalRouteIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel service crashes and triggers the fallback response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel2 service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllTravelsTravelServiceCrash() throws Exception {
        // Arrange
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/admin_trip"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Trip>>>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/admintravelservice/admintravel"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private List<String> extractRouteIdUUIDListFromMVCResult(MvcResult result) throws JSONException, UnsupportedEncodingException {
        JSONObject responseJson = new JSONObject(result.getResponse().getContentAsString());
        JSONArray responseArray =  responseJson.getJSONArray("data");
        ArrayList<String> responseIds = new ArrayList<>();
        for (int i = 0; i < responseArray.length(); i++) {
            JSONObject trip = responseArray.getJSONObject(i).getJSONObject("trip");
            responseIds.add(trip.getString("routeId"));
        }
        return responseIds;
    }

    private List<String> extractRouteIdUUIDListFromTwoAdminTripArrays(List<AdminTrip> adminTripList, List<AdminTrip> adminTrip2List) {
        ArrayList<AdminTrip> expectedTotalAdminTripList = new ArrayList<>();
        expectedTotalAdminTripList.addAll(adminTripList);
        expectedTotalAdminTripList.addAll(adminTrip2List);

        return expectedTotalAdminTripList.stream()
                .map(AdminTrip::getTrip)
                .map(Trip::getRouteId)
                .collect(Collectors.toList());
    }

    private ArrayList<AdminTrip> generateRandomAdminTripList(int length) {
        ArrayList<AdminTrip> adminTripList = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            adminTripList.add(generateRandomAdminTrip());
        }

        return adminTripList;
    }

    private AdminTrip generateRandomAdminTrip() {
        AdminTrip adminTrip = new AdminTrip();
        adminTrip.setTrip(generateRandomTrip());
        return adminTrip;
    }

    private Trip generateRandomTrip() {
        Trip trip = new Trip();
        trip.setRouteId(UUID.randomUUID().toString());
        return trip;
    }
}
