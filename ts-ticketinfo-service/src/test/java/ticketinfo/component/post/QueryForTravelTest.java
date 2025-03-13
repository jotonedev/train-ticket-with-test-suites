package ticketinfo.component.post;

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
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import ticketinfo.entity.Travel;
import ticketinfo.entity.TravelResult;
import ticketinfo.entity.Trip;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryForTravelTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when basic service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-basic-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some travel entity with randomly configured starting and end place
     * <li><b>Expected result:</b></li> The endpoint passes the response from basic service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelBasicServiceSuccessful() throws Exception {
        // Arrange
        TravelResult travelResult = new TravelResult();
        travelResult.setMessage(UUID.randomUUID().toString());

        Trip trip = new Trip();
        Travel info = new Travel();

        info.setTrip(trip);
        info.setStartingPlace(UUID.randomUUID().toString());
        info.setEndPlace(UUID.randomUUID().toString());
        info.setDepartureTime(new Date());

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", travelResult), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/ticketinfoservice/ticketinfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(notNullValue())))
                .andExpect(jsonPath("$.data.message", is(travelResult.getMessage())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when basic service returns a failure response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-basic-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some travel entity with randomly configured starting and end place
     * <li><b>Expected result:</b></li> The endpoint passes the response from basic service to the client, which in this case
     * is a failing response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelBasicServiceFailure() throws Exception {
        // Arrange
        TravelResult travelResult = new TravelResult();
        travelResult.setMessage(UUID.randomUUID().toString());

        Trip trip = new Trip();
        Travel info = new Travel();

        info.setTrip(trip);
        info.setStartingPlace(UUID.randomUUID().toString());
        info.setEndPlace(UUID.randomUUID().toString());
        info.setDepartureTime(new Date());

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-basic-service:15680/api/v1/basicservice/basic/travel"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by basicservice",
                        travelResult), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/ticketinfoservice/ticketinfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Failure message created by basicservice")))
                .andExpect(jsonPath("$.data", is(notNullValue())))
                .andExpect(jsonPath("$.data.message", is(travelResult.getMessage())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TravelResult objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelMultipleObjects() throws Exception {
        // Arrange
        List<TravelResult> travelResults = new ArrayList<>();
        travelResults.add(new TravelResult());
        travelResults.add(new TravelResult());

        String jsonRequest = new ObjectMapper().writeValueAsString(travelResults);

        // Act
        mockMvc.perform(post("/api/v1/ticketinfoservice/ticketinfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
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
    public void testQueryForTravelMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/contactservice/contacts")
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
    public void testQueryForTravelMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/ticketinfoservice/ticketinfo")
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
    public void testQueryForTravelNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/ticketinfoservice/ticketinfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }
}
