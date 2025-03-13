package fdse.microservice.component.get;

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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryForStationIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the station service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random stationName, response of Station is mocked.
     * <li><b>Expected result:</b></li> status 1, msg "Success", the stationId as returned by the station service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdStationServiceSuccess() throws Exception {
        // Arrange
        String expectedStationName = UUID.randomUUID().toString();
        String expectedStationId = UUID.randomUUID().toString();
        Response<String> stationResponse = new Response<>(1, "Success", expectedStationId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + expectedStationName),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(stationResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/basicservice/basic/" + expectedStationName))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(expectedStationId)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the station service returns a failing response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random stationName, response of Station is mocked.
     * <li><b>Expected result:</b></li> status 0, msg "Not exists", the stationName as returned by the station service.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdStationServiceFailure() throws Exception {
        // Arrange
        String expectedStationName = UUID.randomUUID().toString();

        Response<String> stationResponse = new Response<>(0, "Not exists", expectedStationName);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + expectedStationName),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(stationResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/basicservice/basic/" + expectedStationName))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Not exists")))
                .andExpect(jsonPath("$.data", is(expectedStationName)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the station service returns null values
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random stationName, response of Station is mocked.
     * <li><b>Expected result:</b></li> null values for status, msg and data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdStationServiceNull() throws Exception {
        // Arrange
        String expectedStationName = UUID.randomUUID().toString();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + expectedStationName),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(null);

        // Act
        mockMvc.perform(get("/api/v1/basicservice/basic/" + expectedStationName))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/basicservice/basic/{stationName}", UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when nothing is passed to the endpoint
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> an IllegalArgumentException
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdNonExistingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/basicservice/basic/{stationName}"));
        });
    }
}
