package admintravel.component.delete;

import admintravel.entity.TripId;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteTravelTest {

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
     * <li><b>Parameters:</b></li> tripId that starts with G of the to be deleted trip
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelTripIdStartsWithGTravelServiceSuccessful() throws Exception {
        // Arrange
        String expectedTripId = "G" + generateRandomString();
        TripId expectedTId = new TripId(expectedTripId);
        Response<TripId> travelResponse = new Response<>(1, "Delete trip:" + expectedTripId + ".", expectedTId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + expectedTripId + ".")))
                .andExpect(jsonPath("$.data.number", is(expectedTripId.substring(1))));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> tripId that starts with G of the to be deleted trip, and a random UUID
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelMultipleIds() throws Exception {
        // Arrange
        String expectedTripId = "G" + generateRandomString();
        TripId expectedTId = new TripId(expectedTripId);
        Response<TripId> travelResponse = new Response<>(1, "Delete trip:" + expectedTripId + ".", expectedTId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + expectedTripId + ".")))
                .andExpect(jsonPath("$.data.number", is(expectedTripId.substring(1))));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> tripId that starts with D of the to be deleted trip
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelTripIdStartsWithDTravelServiceSuccessful() throws Exception {
        // Arrange
        String expectedTripId = "D" + generateRandomString();
        TripId expectedTId = new TripId(expectedTripId);
        Response<TripId> travelResponse = new Response<>(1, "Delete trip:" + expectedTripId + ".", expectedTId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + expectedTripId + ".")))
                .andExpect(jsonPath("$.data.number", is(expectedTripId.substring(1))));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when travel2 service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> tripId that neither starts with G nor D of the to be deleted trip
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel2 service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelTripIdNotStartsWithDOrGTravel2ServiceSuccessful() throws Exception {
        // Arrange
        String expectedTripId = "A" + generateRandomString();
        TripId expectedTId = new TripId(expectedTripId);
        Response<TripId> travelResponse = new Response<>(1, "Delete trip:" + expectedTripId + ".", expectedTId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete trip:" + expectedTripId + ".")))
                .andExpect(jsonPath("$.data.number", is(expectedTripId.substring(1))));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> tripId that starts with D or G of the to be deleted trip
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel service to the client, which in this case
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelTripIdStartsWithGOrDTravelServiceCrash() throws Exception {
        // Arrange
        String expectedTripId = "D" + generateRandomString();
        Response<TripId> travelResponse = new Response<>(null, null, null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId))
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
     * <li><b>Parameters:</b></li> tripId that neither starts with G nor D of the to be deleted trip
     * <li><b>Expected result:</b></li> The endpoint passes the response from the travel2 service to the client, which in this case
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelTripIdNotyStartsWithGOrDTravel2ServiceCrash() throws Exception {
        // Arrange
        String expectedTripId = "A" + generateRandomString();
        Response<TripId> travelResponse = new Response<>(null, null, null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips/" + expectedTripId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(travelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", expectedTripId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTravelMalformedId() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}", UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testDeleteTravelMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/admintravelservice/admintravel/{tripId}"));
        });
    }

    private String generateRandomString() {
        StringBuilder stringBuilder = new StringBuilder(20);
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            stringBuilder.append(CHARACTERS.charAt(randomIndex));
        }

        return stringBuilder.toString();
    }
}
