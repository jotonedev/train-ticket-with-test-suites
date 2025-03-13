package fdse.microservice.component.post;

import edu.fudan.common.util.Response;
import fdse.microservice.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class QueryForTravelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when all called services return a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid travel Object. All mocked services return a successful response
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TravelResult
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelAllServicesSuccess() throws Exception {
        // Arrange
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 1.0, 2.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingStation", "endStation"));
        route.setDistances(Arrays.asList(0, 1));

        Response<String> stationResponse1 = new Response<>(1, "Success", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));


        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(true);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travel)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(travelResult.isStatus()))
                .andExpect(jsonPath("$.data.trainType").value(travelResult.getTrainType()))
                .andExpect(jsonPath("$.data.percent").value(travelResult.getPercent()))
                .andExpect(jsonPath("$.data.prices.confortClass", is("2.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("1.0")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the station service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid travel Object. The mocked station service returns a failure response
     * <li><b>Expected result:</b></li> status 0, msg "Start place or end place not exist!", a configured TravelResult with a false status
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelStationServiceFailure() throws Exception {
        // Arrange
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("NotstartingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("NotstartingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse1 = new Response<>(0, "Not exists", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/NotstartingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/NotstartingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(false);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travel)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.msg").value("Start place or end place not exist!"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").value(trainType))
                .andExpect(jsonPath("$.data.percent").value(1.0));


        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the train service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid travel Object. The mocked train service returns a failure response
     * <li><b>Expected result:</b></li> status 0, msg "Train type doesn't exist", a configured TravelResult with a false status
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelTrainServiceFailure() throws Exception {
        // Arrange
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        Route route = new Route();
        String trainType = "";
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType, trip.getRouteId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse = new Response<>(1, "Success", trip.getStationsId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<Object> trainResponse = new Response<>(0, "here is no TrainType with the trainType id: " + trip.getTrainTypeId(), null);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trainType))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(false);
        travelResult.setPercent(1.0);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travel)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.msg").value("Train type doesn't exist"))
                .andExpect(jsonPath("$.data.status").value(false))
                .andExpect(jsonPath("$.data.trainType").isEmpty())
                .andExpect(jsonPath("$.data.percent").value(1.0));


        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the route service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid travel Object. The mocked route service returns a failure response
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TravelResult that has ticket prices of 0
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelRouteServiceFailure() throws Exception {
        // Arrange
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse1 = new Response<>(1, "Success", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(0, "No content with the routeId", null);
        Response<PriceConfig> priceConfigResponse = new Response<>(1, "Success", priceConfig);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));


        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(true);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travel)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(travelResult.isStatus()))
                .andExpect(jsonPath("$.data.trainType").value(travelResult.getTrainType()))
                .andExpect(jsonPath("$.data.percent").value(travelResult.getPercent()))
                .andExpect(jsonPath("$.data.prices.confortClass", is("0.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("0.0")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the price service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-station-service</li>
     *   <li>ts-train-service</li>
     *   <li>ts-route-service</li>
     *   <li>ts-price-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> A valid travel Object. The mocked price service returns a failure response
     * <li><b>Expected result:</b></li> status 1, msg "Success", a configured TravelResult with default prices for the ticket
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelPriceServiceFailure() throws Exception {
        // Arrange
        Travel travel = new Travel();
        Trip trip = new Trip(new TripId("G1234"), "trainTypeId", UUID.randomUUID().toString());
        TrainType trainType = new TrainType(trip.getTrainTypeId(), 1, 2);
        Route route = new Route();
        PriceConfig priceConfig = new PriceConfig(UUID.randomUUID(), trainType.toString(), route.getId(), 50.0, 100.0);

        trip.setStartingStationId("startingStation");
        trip.setStationsId("stationsId");
        trip.setTerminalStationId("endStation");

        travel.setStartingPlace("startingPlace");
        travel.setEndPlace("endPlace");
        travel.setDepartureTime(new Date());
        travel.setTrip(trip);

        route.setId(trip.getRouteId());
        route.setStations(Arrays.asList("startingPlace", "endPlace"));
        route.setDistances(Arrays.asList(0, 100));

        Response<String> stationResponse1 = new Response<>(1, "Success", trip.getStartingStationId());
        Response<String> stationResponse2 = new Response<>(1, "Success", trip.getTerminalStationId());
        Response<TrainType> trainResponse = new Response<>(1, "success", trainType);
        Response<Route> routeResponse = new Response<>(1, "Success", route);
        Response<PriceConfig> priceConfigResponse = new Response<>(0, "No that config", null);

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));

        mockServer.expect(requestTo("http://ts-train-service:14567/api/v1/trainservice/trains/trainTypeId"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(trainResponse)));

        mockServer.expect(requestTo("http://ts-route-service:11178/api/v1/routeservice/routes/" + trip.getRouteId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(routeResponse)));

        mockServer.expect(requestTo("http://ts-price-service:16579/api/v1/priceservice/prices/" + trip.getRouteId() + "/" + trip.getTrainTypeId()))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(priceConfigResponse)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/startingPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse1)));

        mockServer.expect(requestTo("http://ts-station-service:12345/api/v1/stationservice/stations/id/endPlace"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(stationResponse2)));


        TravelResult travelResult = new TravelResult();
        travelResult.setStatus(true);
        travelResult.setTrainType(trainType);
        travelResult.setPercent(1.0);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(travel)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.status").value(travelResult.isStatus()))
                .andExpect(jsonPath("$.data.trainType").value(travelResult.getTrainType()))
                .andExpect(jsonPath("$.data.percent").value(travelResult.getPercent()))
                .andExpect(jsonPath("$.data.prices.confortClass", is("120.0")))
                .andExpect(jsonPath("$.data.prices.economyClass", is("95.0")));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Travel objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testQueryForTravelMultipleObjects() throws Exception {
        // Arrange
        Travel[] orders = {new Travel(), new Travel()};
        String jsonRequest = new ObjectMapper().writeValueAsString(orders);

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
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
        String requestJson = "{\"id\":\"1id\", \"name\":not, \"value\":valid, \"description\":type}";

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
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
    public void testQueryForTravelEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
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
    public void testQueryForTravelNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/basicservice/basic/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }
}
