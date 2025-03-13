package adminroute.component.post;

import adminroute.entity.RouteInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AddRouteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when route service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added route info object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the route service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddRouteRouteServiceSuccess() throws Exception {
        // Arrange
        RouteInfo expectedRouteInfo = generateRandomRouteInfo();
        Response<RouteInfo> routeResponse = new Response<>(1, "Save success", expectedRouteInfo);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<RouteInfo>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Response<RouteInfo>>>any()))
                .thenReturn(new ResponseEntity<>(routeResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedRouteInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Save success")))
                .andExpect(jsonPath("$.data.id", is(expectedRouteInfo.getId().toString())))
                .andExpect(jsonPath("$.data.stationList", is(expectedRouteInfo.getStationList())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added route info object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the route service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddRouteRouteServiceCrash() throws Exception {
        // Arrange
        RouteInfo expectedRouteInfo = generateRandomRouteInfo();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<RouteInfo>>any(),
                ArgumentMatchers.<ParameterizedTypeReference<Response<RouteInfo>>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedRouteInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two RouteInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddRouteMultipleObjects() throws Exception {
        // Arrange
        List<RouteInfo> travelResults = new ArrayList<>();
        travelResults.add(new RouteInfo());
        travelResults.add(new RouteInfo());

        String jsonRequest = new ObjectMapper().writeValueAsString(travelResults);

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
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
    public void testAddRouteMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
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
    public void testAddRouteMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
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
    public void testAddRouteNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private RouteInfo generateRandomRouteInfo() {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setId(UUID.randomUUID().toString());
        routeInfo.setStationList(UUID.randomUUID().toString());
        return routeInfo;
    }
}
