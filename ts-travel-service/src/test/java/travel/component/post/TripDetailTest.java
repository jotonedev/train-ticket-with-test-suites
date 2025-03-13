package travel.component.post;

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
import travel.entity.*;
import travel.repository.TripRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
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
public class TripDetailTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty or contains no trip element that matches the trip id
     * of TripAllDetailInfo in the request body
     * <li><b>Parameters:</b></li> a fully configured TripAllDetailInfo object with a trip id that does not exist in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetail with its trip response and trip set to null
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoEmptyDatabase() throws Exception {
        // Arrange
        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse", nullValue()))
                .andExpect(jsonPath("$.data.trip", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body. However, TripAllDetailInfo was not fully defined,
     * meaning endPlace and / or startingPlace are null
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a TripAllDetailInfo entity with a trip id that exists in the database, but the startingPlace and
     * endPlace are null
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoFromToNotDefined() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();
        tripAllDetailInfo.setTripId(storedTrip.getTripId().toString());
        tripAllDetailInfo.setFrom(null);
        tripAllDetailInfo.setTo(null);

        // When restTemplate.exchange() is called with the specific URL and null as the stationName,
        // then the fallback response is returned.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response<String>>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null,
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body, starting and end place have been defined and exist as stations in the
     * database. However, the departure time within TripAllDetailInfo is not defined, meaning it is null
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a TripAllDetailInfo entity with a trip id that exists in the database, but the departure time is null
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> departure time is never null-checked, leading to a NullPointerException.
     * As the fallback response is triggered with a 200 OK, it remains unclear whether
     * developers deliberately omitted preventative measures (e.g. null-checks) in favor of relying on the
     * fallback (which would be considered intended behavior), or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoDepartureTimeNotDefined() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();
        tripAllDetailInfo.setTripId(storedTrip.getTripId().toString());
        tripAllDetailInfo.setTravelDate(null);

        // ticketinfoservice will be called. It can just return some random stationId.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        UUID.randomUUID().toString()), HttpStatus.OK));

        // routeservice will be called. It should just return some route.
        Route route = new Route();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body, starting and end place have been defined and exist as stations in the
     * database. However, the provided departure time is not today or after today
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a TripAllDetailInfo entity with a trip id that exists in the database, but the departure time is set
     * to a date in the past
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetail with its trip response and trip set to null
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoDepartureTimeFromPast() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();
        tripAllDetailInfo.setTripId(storedTrip.getTripId().toString());

        Date dateFromPast = new Date(0); // 1970-01-01 00:00:00
        tripAllDetailInfo.setTravelDate(dateFromPast);

        // ticketinfoservice will be called. It can just return some random stationId.
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-ticketinfo-service:15681/api/v1/ticketinfoservice/ticketinfo/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        UUID.randomUUID().toString()), HttpStatus.OK));

        // routeservice will be called. It should just return some route.
        Route route = new Route();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-route-service:11178/api/v1/routeservice/routes/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
                        route), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse", nullValue()))
                .andExpect(jsonPath("$.data.trip", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> hhow the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body, starting and end place have been defined and exist as stations in the
     * database. Finally, the departure time within TripAllDetailInfo is defined and is specified for today or after
     * today. However, there are no available tickets for the specified departure time
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripAllDetailInfo object with a trip id that exists in the database
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The result of the order service is not null-checked, which leads to a
     * NullPointerException. As the fallback response is triggered with a 200 OK, it remains unclear whether
     * developers deliberately omitted preventative measures (e.g. null-checks) in favor of relying on the
     * fallback (which would be considered intended behavior), or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoSoldTicketsNotExist() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();
        tripAllDetailInfo.setTripId(storedTrip.getTripId().toString());

        /// The ticketinfoservice is called on multiple instances. The first call will return the starting placeId and
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

        // routeservice will be called. It should just return some route.
        Route route = new Route();
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
                .thenReturn(new ResponseEntity<>(new Response<>(0, "Failure message created by orderotherservice",
                        null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database contains a trip element that matches the trip id of
     * TripAllDetailInfo in the request body, starting and end place have been defined and exist as stations in the
     * database. Finally, the departure time within TripAllDetailInfo is defined and is specified for today or after
     * today and there are available tickets for the specified departure time
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-ticketinfo-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-order-service</li>
     *   <li>ts-seat-service</li>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a fully configured TripAllDetailInfo object with a trip id that exists in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", a TripAllDetailInfo with its configured trip response.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetTripAllDetailInfoFullyValidRequestBody() throws Exception {
        // Arrange
        Trip storedTrip = populateDatabase();
        assertEquals(1L, tripRepository.count());

        TripAllDetailInfo tripAllDetailInfo = configureTripAllDetailInfo();
        tripAllDetailInfo.setTripId(storedTrip.getTripId().toString());

        // The ticketinfoservice is called on multiple instances. The third call requires a request body and will therefore
        // return an instance of TravelResult.
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

        // routeservice will be called. It should return a route where within the station list, starting and ending station
        // are not in order.
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
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.isA(HttpEntity.class),
                ArgumentMatchers.any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new Response<>(1, "Success",
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
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tripAllDetailInfo)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.tripResponse.startingStation", is(tripAllDetailInfo.getFrom())))
                .andExpect(jsonPath("$.data.tripResponse.terminalStation", is(tripAllDetailInfo.getTo())))
                .andExpect(jsonPath("$.data.tripResponse.economyClass", is(restNumber)))
                .andExpect(jsonPath("$.data.tripResponse.confortClass", is(restNumber)))
                .andExpect(jsonPath("$.data.tripResponse.priceForEconomyClass", is(travelResult.getPrices().get("economyClass"))))
                .andExpect(jsonPath("$.data.tripResponse.priceForConfortClass", is(travelResult.getPrices().get("confortClass"))))
                .andExpect(jsonPath("$.data.trip.tripId.type", is(storedTrip.getTripId().getType().toString())))
                .andExpect(jsonPath("$.data.trip.tripId.number", is(storedTrip.getTripId().getNumber())))
                .andExpect(jsonPath("$.data.trip.routeId", is(storedTrip.getRouteId())));
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
    public void testGetTripAllDetailInfoMultipleObjects() throws Exception {
        // Arrange
        TripAllDetailInfo[] info = {new TripAllDetailInfo(), new TripAllDetailInfo()};
        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
    public void testGetTripAllDetailInfoMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"some\":wrong, \"request\":json, \"string\":content}";

        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
    public void testGetTripAllDetailInfoEmptyBody() throws Exception {
        // Act
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
    public void testGetTripAllDetailInfoNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/travelservice/trip_detail")
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
        tripId.setType(Type.G);
        tripId.setNumber(UUID.randomUUID().toString());
        trip.setTripId(tripId);
        trip.setRouteId(UUID.randomUUID().toString());
        trip.setStartingTime(new Date());
        return tripRepository.save(trip);
    }

    private TripAllDetailInfo configureTripAllDetailInfo() {
        TripAllDetailInfo tripAllDetailInfo = new TripAllDetailInfo();
        tripAllDetailInfo.setTripId(UUID.randomUUID().toString());
        tripAllDetailInfo.setFrom(UUID.randomUUID().toString());
        tripAllDetailInfo.setTo(UUID.randomUUID().toString());
        tripAllDetailInfo.setTravelDate(new Date());
        return tripAllDetailInfo;
    }

    private TravelResult configureTravelResult() {
        TravelResult travelResult = new TravelResult();

        Map<String, String> prices = new HashMap<>();
        prices.put("confortClass", "100");
        prices.put("economyClass", "50");

        travelResult.setPrices(prices);

        return travelResult;
    }

    private SoldTicket configureSoldTicket() {
        SoldTicket soldTicket = new SoldTicket();
        soldTicket.setTrainNumber(UUID.randomUUID().toString());
        return soldTicket;
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
}
