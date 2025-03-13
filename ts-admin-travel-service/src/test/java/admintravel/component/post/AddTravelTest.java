package admintravel.component.post;

import admintravel.entity.TravelInfo;
import admintravel.entity.Trip;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AddTravelTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that starts with G
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdStartsWithGTravelServiceSuccessful() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainTypeId = "G" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainTypeId);
        Response<TravelInfo> travelResponse = new Response<>(1, "[Admin Travel Service][Admin add new travel]", expectedTravelInfo);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Admin Travel Service][Admin add new travel]")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that starts with G or D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a failure response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdStartsWithGOrDTravelServiceFailure() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainTypeId = "G" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainTypeId);
        Response<TravelInfo> travelResponse = new Response<>(0, "Admin add new travel failed", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Admin add new travel failed")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that neither starts with G nor D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a failure response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdNOTStartsWithGOrDTravelServiceFailure() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainTypeId = "A" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainTypeId);
        Response<TravelInfo> travelResponse = new Response<>(0, "Admin add new travel failed", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Admin add new travel failed")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TravelInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelMultipleObjects() throws Exception {
        List<TravelInfo> travelInfoList = new ArrayList<>();
        travelInfoList.add(new TravelInfo());
        travelInfoList.add(new TravelInfo());

        String requestJson = new ObjectMapper().writeValueAsString(travelInfoList);

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
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
    public void testAddTravelTripMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
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
    public void testAddTravelTripMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
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
    public void testAddTravelTripNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
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
     * <li><b>Tests:</b></li> how the endpoint behaves when travel service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that starts with D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdStartsWithDTravelServiceSuccessful() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainTypeId = "D" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainTypeId);
        Trip expectedTrip = generateRandomTrip();
        Response<TravelInfo> travelResponse = new Response<>(1, "[Admin Travel Service][Admin add new travel]", expectedTravelInfo);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Admin Travel Service][Admin add new travel]")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel2 service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that neither starts with G nor D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel2 service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdNotStartsWithDOrGTravel2ServiceSuccessful() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainTypeId = "A" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainTypeId);
        Response<TravelInfo> travelResponse = new Response<>(1, "[Admin Travel Service][Admin add new travel]", expectedTravelInfo);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("[Admin Travel Service][Admin add new travel]")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that starts with G or D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdStartsWithGOrDTravelServiceCrash() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainNumber = "D" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainNumber);
        Response<TravelInfo> travelResponse = new Response<>(null, null, null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Arrange
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added travel info object with a train type that neither starts with G nor D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel2 service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddTravelTripIdNotyStartsWithGOrDTravel2ServiceCrash() throws Exception {
        // Arrange
        TravelInfo expectedTravelInfo = generateRandomTravelInfo();
        String expectedTrainNumber = "A" + generateRandomString(10);
        expectedTravelInfo.setTrainTypeId(expectedTrainNumber);
        Response<TravelInfo> travelResponse = new Response<>(null, null, null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<TravelInfo>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Arrange
        mockMvc.perform(post("/api/v1/admintravelservice/admintravel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private String generateRandomString(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            stringBuilder.append(CHARACTERS.charAt(randomIndex));
        }

        return stringBuilder.toString();
    }

    private Trip generateRandomTrip() {
        Trip trip = new Trip();
        trip.setRouteId(UUID.randomUUID().toString());
        return trip;
    }

    private TravelInfo generateRandomTravelInfo() {
        return new TravelInfo();
    }
}
