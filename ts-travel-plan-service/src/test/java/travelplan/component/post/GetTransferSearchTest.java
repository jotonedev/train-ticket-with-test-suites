package travelplan.component.post;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import travelplan.entity.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetTransferSearchTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Container
    public static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:4.0.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        String mongoUri = mongo.getReplicaSetUrl();
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel services return a successful response.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid TransferTravelInfo Object. The mocked travel service returns a successful response with some list of TripResponses.
     * <li><b>Expected result:</b></li> status 1, msg "Success.", a TransferTravelResult with the combined lists of the two travel services.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTransferSearchTravelServiceSuccess() throws Exception {
        // Arrange
        TransferTravelInfo transferTravelInfo = configureTransferTravelInfo();

        ArrayList<TripResponse> emptyTripResponses = new ArrayList<>();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/left"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        emptyTripResponses), HttpStatus.OK)); // returns an empty list

        TripResponse tripResponse = new TripResponse();
        tripResponse.setTripId(new TripId(UUID.randomUUID().toString()));
        ArrayList<TripResponse> tripResponses = new ArrayList<>();
        tripResponses.add(tripResponse);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips/left"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        tripResponses), HttpStatus.OK)); // returns a list with one element

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(transferTravelInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", is(notNullValue())))
                .andExpect(jsonPath("$.data.firstSectionResult").isArray())
                // one of the lists is empty, so the combined list should only contain the non-empty list
                .andExpect(jsonPath("$.data.firstSectionResult", hasSize(1)))
                .andExpect(jsonPath("$.data.firstSectionResult[0]", is(notNullValue())))
                .andExpect(jsonPath("$.data.firstSectionResult[0].tripId.number",
                        is(tripResponse.getTripId().getNumber())))
                .andExpect(jsonPath("$.data.secondSectionResult").isArray())
                // one of the lists is empty, so the combined list should only contain the non-empty list
                .andExpect(jsonPath("$.data.secondSectionResult", hasSize(1)))
                .andExpect(jsonPath("$.data.secondSectionResult[0]", is(notNullValue())))
                .andExpect(jsonPath("$.data.secondSectionResult[0].tripId.number",
                        is(tripResponse.getTripId().getNumber())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the travel services return a failure response.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   <li>ts-travel2-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid TransferTravelInfo Object. The mocked travel service returns a failure response
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTransferSearchTravelServiceFailure() throws Exception {
        // Arrange
        TransferTravelInfo transferTravelInfo = configureTransferTravelInfo();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel-service:12346/api/v1/travelservice/trips/left"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        null), HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-travel2-service:16346/api/v1/travel2service/trips/left"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(transferTravelInfo)))
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
     * <li><b>Parameters:</b></li> Two TransferTravelInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testTransferSearchMultipleObjects() throws Exception {
        // Arrange
        TransferTravelInfo[] orders = {configureTransferTravelInfo(), configureTransferTravelInfo()};
        String jsonRequest = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
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
    public void testTransferSearchMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
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
    public void testTransferSearchEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
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
    public void testTransferSearchNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/transferResult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private TransferTravelInfo configureTransferTravelInfo() {
        TransferTravelInfo transferTravelInfo = new TransferTravelInfo();
        transferTravelInfo.setFromStationName(UUID.randomUUID().toString());
        transferTravelInfo.setViaStationName(UUID.randomUUID().toString());
        transferTravelInfo.setToStationName(UUID.randomUUID().toString());
        transferTravelInfo.setTravelDate(new Date());
        transferTravelInfo.setTrainType(UUID.randomUUID().toString());
        return transferTravelInfo;
    }
}
