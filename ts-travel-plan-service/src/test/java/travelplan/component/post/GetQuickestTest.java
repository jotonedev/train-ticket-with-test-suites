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
import travelplan.entity.RoutePlanResultUnit;
import travelplan.entity.TripInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
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
public class GetQuickestTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the route-plan service returns an empty list
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid TripInfo Object. The mocked route-plan service returns a successful response with and empty list
     * <li><b>Expected result:</b></li> status 0, msg "Cannot Find", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQuickestRouteServiceEmptyList() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo();

        ArrayList<RoutePlanResultUnit> emptyRoutePlanResultUnits = new ArrayList<>();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-plan-service:14578/api/v1/routeplanservice/routePlan/quickestRoute"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        emptyRoutePlanResultUnits), HttpStatus.OK)); // returns an empty list

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Cannot Find")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the route-plan service returns a non-empty list
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-route-plan-service</li>
     *   <li>ts-station-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-ticketinfo-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid TripInfo Object. The mocked route-plan service returns a successful response a list of RoutePlanResultUnits
     * <li><b>Expected result:</b></li> status 1, msg "Success", a list of TravelAdvanceResultUnits
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQuickestRouteServiceNonEmptyList() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo();

        RoutePlanResultUnit routePlanResultUnit = configureRoutePlanResultUnit();
        ArrayList<RoutePlanResultUnit> routePlanResultUnits = new ArrayList<>();
        routePlanResultUnits.add(routePlanResultUnit);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-plan-service:14578/api/v1/routeplanservice/routePlan/quickestRoute"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        routePlanResultUnits), HttpStatus.OK)); // returns a non-empty list

        // stationservice will be called. It will just return a list of strings
        ArrayList<String> stationNames = new ArrayList<>();
        String stationName = UUID.randomUUID().toString();
        stationNames.add(stationName);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-station-service:12345/api/v1/stationservice/stations/namelist"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        stationNames), HttpStatus.OK));

        // seatservice will be called. It should just return some arbitrary number.
        int restNumber = 10;
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        restNumber), HttpStatus.OK));

        // ticketinfoservice will be called. It can just return some random stationId.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        UUID.randomUUID().toString()), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tripId", is(routePlanResultUnit.getTripId())))
                .andExpect(jsonPath("$.data[0].trainTypeId", is(routePlanResultUnit.getTrainTypeId())))
                .andExpect(jsonPath("$.data[0].fromStationName", is(routePlanResultUnit.getFromStationName())))
                .andExpect(jsonPath("$.data[0].toStationName", is(routePlanResultUnit.getToStationName())))
                .andExpect(jsonPath("$.data[0].stopStations[0]", is(stationName)))
                .andExpect(jsonPath("$.data[0].priceForSecondClassSeat", is(routePlanResultUnit.getPriceForSecondClassSeat())))
                .andExpect(jsonPath("$.data[0].priceForFirstClassSeat", is(routePlanResultUnit.getPriceForFirstClassSeat())))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketSecondClass", is(restNumber)))
                .andExpect(jsonPath("$.data[0].numberOfRestTicketFirstClass", is(restNumber)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TripInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQuickestMultipleObjects() throws Exception {
        // Arrange
        TripInfo[] orders = {configureTripInfo(), configureTripInfo()};
        String jsonRequest = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
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
    public void testGetQuickestMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
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
    public void testGetQuickestEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
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
    public void testGetQuickestNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travelplanservice/travelPlan/quickest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private TripInfo configureTripInfo() {
        TripInfo tripInfo = new TripInfo();
        tripInfo.setStartingPlace(UUID.randomUUID().toString());
        tripInfo.setEndPlace(UUID.randomUUID().toString());
        tripInfo.setDepartureTime(new Date());
        return tripInfo;
    }

    private RoutePlanResultUnit configureRoutePlanResultUnit() {
        RoutePlanResultUnit routePlanResultUnit = new RoutePlanResultUnit();
        routePlanResultUnit.setTripId(UUID.randomUUID().toString());
        routePlanResultUnit.setTrainTypeId(UUID.randomUUID().toString());
        routePlanResultUnit.setFromStationName(UUID.randomUUID().toString());
        routePlanResultUnit.setToStationName(UUID.randomUUID().toString());
        routePlanResultUnit.setStartingTime(new Date());
        routePlanResultUnit.setEndTime(new Date());
        routePlanResultUnit.setPriceForFirstClassSeat(UUID.randomUUID().toString());
        routePlanResultUnit.setPriceForSecondClassSeat(UUID.randomUUID().toString());

        ArrayList<String> stopStations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            stopStations.add(UUID.randomUUID().toString());
        }

        routePlanResultUnit.setStopStations(stopStations);
        return routePlanResultUnit;
    }
}
