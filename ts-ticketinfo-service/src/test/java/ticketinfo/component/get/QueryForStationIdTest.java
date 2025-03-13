package ticketinfo.component.get;

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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryForStationIdTest {

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
     * <li><b>Parameters:</b></li> random station name
     * <li><b>Expected result:</b></li> The endpoint passes the response from basic service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdBasicServiceSuccessful() throws Exception {
        // Arrange
        String stationName = UUID.randomUUID().toString();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-basic-service:15680/api/v1/basicservice/basic/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", stationName), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(stationName)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when basic service returns a failure response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-basic-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> random station name
     * <li><b>Expected result:</b></li> The endpoint passes the response from basic service to the client, which in this case
     * is a failing response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdBasicServiceFailure() throws Exception {
        // Arrange
        String stationName = UUID.randomUUID().toString();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-basic-service:15680/api/v1/basicservice/basic/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by basicservice",
                        stationName), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Failure message created by basicservice")))
                .andExpect(jsonPath("$.data", is(stationName)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-basic-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> random station name and additional random strings
     * <li><b>Expected result:</b></li> The endpoint passes the response from basic service to the client, which in this case
     * is a successful response. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForStationIdMultipleIds() throws Exception {
        // Arrange
        String stationName = UUID.randomUUID().toString();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-basic-service:15680/api/v1/basicservice/basic/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success", stationName), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/ticketinfoservice/ticketinfo/{name}", stationName, UUID.randomUUID().toString()))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is(stationName)));
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
    public void testCheckMalformedId() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/ticketinfoservice/ticketinfo/{name}", UUID.randomUUID() + "/" + UUID.randomUUID()))
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
    public void testCheckMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/ticketinfoservice/ticketinfo/{name}"));
        });
    }
}
