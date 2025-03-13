package travel2.component.post;

import edu.fudan.common.util.Response;
import org.junit.jupiter.api.BeforeEach;
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
import travel2.entity.*;
import travel2.repository.TripRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TripsLeftTest {

    @Autowired
    private TripRepository tripRepository;

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

    @BeforeEach
    void beforeEach() {
        tripRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is invalid, meaning it contains attributes
     * which are either null or empty
     * <li><b>Parameters:</b></li> a TripInfo object with null or empty attributes
     * <li><b>Expected result:</b></li> status 200 OK, empty list
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryTripInfoInvalid() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new TripInfo())))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is valid, but the database is empty and
     * contains no trip element
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object (empty database)
     * <li><b>Expected result:</b></li> status 1, msg "Success", an empty list of TripResponses.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryEmptyDatabase() throws Exception {
        // Arrange
        TripInfo tripInfo = configureTripInfo();

        // ticketinfoservice will be called. It can just return some random stationId.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        UUID.randomUUID().toString()), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success Query")))
                .andExpect(jsonPath("$.data", is(empty())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is valid, but the provided
     * startingPlace and / or endPlace does not exist as a station in the database. The route service will always return some route.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object
     * <li><b>Expected result:</b></li> status 1, msg "Success", an empty list of TripResponses.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStartingAndEndPlaceNotExistRouteServiceSuccessful() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();

        // ticketinfoservice will be called. starting and end place won't be found in the database,
        // so it will return null.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        null), HttpStatus.OK));

        // routeservice is called and always returns some route.
        Route route = configureRoute();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success Query")))
                .andExpect(jsonPath("$.data", is(empty())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is valid, but the provided
     * startingPlace and / or endPlace does not exist as a station in the database. The route service will always fail.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStartingAndEndPlaceNotExistRouteServiceFailure() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();

        // ticketinfoservice will be called. starting and end place won't be found in the database,
        // so it will return null.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        null), HttpStatus.OK));

        // routeservice is called, but cannot find a route at least once.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Route not found",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the  trip info in the request body is valid, the provided starting and end
     * places exist as stations in the database and a route is always found by the mocked routeservice. However,
     * there is no route where the startingPlace comes before the endPlace of stations
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object
     * <li><b>Expected result:</b></li> status 1, msg "Success", an empty list of TripResponses.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryStartingNeverBeforeEndPlace() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();

        // ticketinfoservice will be called twice. The first call will return the starting placeId and the second call
        // will return the ending placeId.
        AtomicInteger callAmount = new AtomicInteger(0);
        String startingPlaceId = UUID.randomUUID().toString();
        String endingPlaceId = UUID.randomUUID().toString();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> {
                    if (callAmount.getAndIncrement() == 0) { // first call
                        return new ResponseEntity<>(new Response<>(1, "Success", startingPlaceId),
                                HttpStatus.OK);
                    } else { // all other calls
                        return new ResponseEntity<>(new Response<>(1, "Success", endingPlaceId),
                                HttpStatus.OK);
                    }
                });

        // routeservice is called and returns a route where the starting place is after the ending place.
        Route route = configureRoute();
        route.getStations().add(endingPlaceId);
        route.getStations().add(startingPlaceId);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success Query")))
                .andExpect(jsonPath("$.data", is(empty())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is valid, the provided starting and end
     * places exist as stations in the database and a route is always found by the mocked routeservice. However, the
     * provided departure time is not today or after today
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object with a departure time that is not today or after today
     * <li><b>Expected result:</b></li> status 0, msg "No Trip info content", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryDepartureTimeFromPast() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();
        Date dateFromPast = new Date(0); // 1970-01-01 00:00:00
        tripInfo.setDepartureTime(dateFromPast);

        // ticketinfoservice will be called twice. The first call will return the starting placeId and the second call
        // will return the ending placeId.
        AtomicInteger callAmount = new AtomicInteger(0);
        String startingPlaceId = UUID.randomUUID().toString();
        String endingPlaceId = UUID.randomUUID().toString();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> {
                    if (callAmount.getAndIncrement() == 0) { // first call
                        return new ResponseEntity<>(new Response<>(1, "Success", startingPlaceId),
                                HttpStatus.OK);
                    } else { // all other calls
                        return new ResponseEntity<>(new Response<>(1, "Success", endingPlaceId),
                                HttpStatus.OK);
                    }
                });

        // routeservice is called and returns a route where starting and ending place are both within the staion
        // list of the route and the starting place is before the ending place.
        Route route = configureRoute();
        route.getStations().add(startingPlaceId);
        route.getStations().add(endingPlaceId);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No Content")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the The trip info in the request body is valid, the provided starting and
     * end places exist as stations in the database, a route is always found by the mocked routeservice and the
     * departure time is specified for today or after today. However, there are no available tickets for the specified
     * departure time
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The result of the order service is not null-checked, which leads to a
     * NullPointerException. As the fallback response is triggered with a 200 OK, it remains unclear whether
     * developers deliberately omitted preventative measures (e.g. null-checks) in favor of relying on the
     * fallback (which would be considered intended behavior), or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQuerySoldTicketsNotExist() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();

        // The ticketinfoservice is called on multiple instances. The first call will return the starting placeId and
        // the second call will return the ending placeId.
        AtomicInteger callAmount = new AtomicInteger(0);
        String startingPlaceId = UUID.randomUUID().toString();
        String endingPlaceId = UUID.randomUUID().toString();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> {
                    if (callAmount.getAndIncrement() == 0) { // first call
                        return new ResponseEntity<>(new Response<>(1, "Success", startingPlaceId),
                                HttpStatus.OK);
                    } else { // second and all other calls after.
                        return new ResponseEntity<>(new Response<>(1, "Success", endingPlaceId),
                                HttpStatus.OK);
                    }
                });

        // the third call of ticketinfoservice. Will return a TravelResult object.
        TravelResult travelResult = configureTravelResult();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(Response.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        travelResult), HttpStatus.OK));

        // routeservice is called and returns a route where starting and ending place are both within the staion
        // list of the route and the starting place is before the ending place.
        Route route = configureRoute();
        route.getStations().add(startingPlaceId);
        route.getStations().add(endingPlaceId);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // orderotherservice will be called. However, nothing is found for the specified departureTime. This service
        // will therefore return null.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by orderservice",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the trip info in the request body is valid, the provided starting and end
     * places exist as stations in the database, a route is always found by the mocked routeservice and the departure
     * time is specified for today or after today
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripInfo object
     * <li><b>Expected result:</b></li> status 1, msg "Success", a list of TripResponses.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryFullyValidRequestBody() throws Exception {
        // Arrange
        populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripInfo tripInfo = configureTripInfo();

        // The ticketinfoservice is called on multiple instances. The third call requires a request body and will therefore
        // return an instance of TravelResult.
        AtomicInteger callAmount = new AtomicInteger(0);
        TravelResult travelResult = configureTravelResult();
        String startingPlaceId = UUID.randomUUID().toString();
        String endingPlaceId = UUID.randomUUID().toString();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> {
                    int amount = callAmount.getAndIncrement();
                    if (amount == 0) { // first call
                        return new ResponseEntity<>(new Response<>(1, "Success", startingPlaceId), HttpStatus.OK);
                    } else if (amount == 1) { // second call
                        return new ResponseEntity<>(new Response<>(1, "Success", endingPlaceId), HttpStatus.OK);
                    } else if (amount == 2) { // third call
                        return new ResponseEntity<>(new Response<>(1, "Success", travelResult), HttpStatus.OK);
                    } else { // all other calls
                        return new ResponseEntity<>(new Response<>(1, "Success", UUID.randomUUID().toString()), HttpStatus.OK);
                    }
                });

        // routeservice is called and returns a route where starting and ending place are both within the staion
        // list of the route and the starting place is before the ending place.
        Route route = configureRoute();
        route.getStations().add(startingPlaceId);
        route.getStations().add(endingPlaceId);
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // orderotherservice will be called. It should just return some sold ticket.
        SoldTicket soldTicket = configureSoldTicket();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by orderotherservice",
                        soldTicket), HttpStatus.OK));

        // seatservice will be called. It should just return some arbitrary number.
        int restNumber = 10;
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        restNumber), HttpStatus.OK));

        // trainservice will be called. It should just return some train type.
        TrainType trainType = configureTrainType();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        trainType), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success Query")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].startingStation", is(tripInfo.getStartingPlace())))
                .andExpect(jsonPath("$.data[0].terminalStation", is(tripInfo.getEndPlace())))
                .andExpect(jsonPath("$.data[0].economyClass", is(restNumber)))
                .andExpect(jsonPath("$.data[0].confortClass", is(restNumber)))
                .andExpect(jsonPath("$.data[0].priceForEconomyClass", is(travelResult.getPrices().get("economyClass"))))
                .andExpect(jsonPath("$.data[0].priceForConfortClass", is(travelResult.getPrices().get("confortClass"))));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TravelInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    void testQueryMultipleObjects() throws Exception {
        // Assert
        TripInfo[] info = {new TripInfo(), new TripInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a bad request
     * </ul>
     * @throws Exception
     */
    @Test
    void testQueryMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"some\":wrong, \"request\":json, \"string\":content}";

        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a bad request.
     * </ul>
     * @throws Exception
     */
    @Test
    void testQueryEmptyBody() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
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
    public void testQueryNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travel2service/trips/left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private Trip populateDatabase() {
        Trip trip = new Trip();
        TripId tripId = new TripId();
        tripId.setType(Type.Z);
        tripId.setNumber(UUID.randomUUID().toString());
        trip.setTripId(tripId);
        trip.setRouteId(UUID.randomUUID().toString());
        trip.setStartingTime(new Date());
        return tripRepository.save(trip);
    }

    private Route configureRoute() {
        Route route = new Route();
        route.setId(UUID.randomUUID().toString());
        route.setStartStationId(UUID.randomUUID().toString());
        route.setTerminalStationId(UUID.randomUUID().toString());

        ArrayList<String> stations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            stations.add(UUID.randomUUID().toString());
        }

        ArrayList<Integer> distances = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            distances.add(i);
        }

        route.setStations(stations);
        route.setDistances(distances);
        return route;
    }

    private TrainType configureTrainType() {
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        trainType.setConfortClass(1);
        trainType.setEconomyClass(2);
        trainType.setAverageSpeed(100);
        return trainType;
    }

    private SoldTicket configureSoldTicket() {
        SoldTicket soldTicket = new SoldTicket();
        soldTicket.setTrainNumber(UUID.randomUUID().toString());
        return soldTicket;
    }

    private TravelResult configureTravelResult() {
        TravelResult travelResult = new TravelResult();

        Map<String, String> prices = new HashMap<>();
        prices.put("confortClass", "100");
        prices.put("economyClass", "50");

        travelResult.setPrices(prices);

        return travelResult;
    }

    private TripInfo configureTripInfo() {
        TripInfo tripInfo = new TripInfo();
        tripInfo.setStartingPlace(UUID.randomUUID().toString());
        tripInfo.setEndPlace(UUID.randomUUID().toString());
        tripInfo.setDepartureTime(new Date());
        return tripInfo;
    }
}
