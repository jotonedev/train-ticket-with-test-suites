package adminorder.component.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.fudan.common.util.Response;
import foodsearch.FoodApplication;
import foodsearch.entity.*;
import foodsearch.repository.FoodOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FoodApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllFoodTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

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
        foodOrderRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give it valid parameters such that all called services return valid responses
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-food-map-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some arbitrary strings for the date, startStation, endStation, and tripId. The input
     * only affects the behavior of the called services, which are mocked.
     * <li><b>Expected result:</b></li> status 1, msg "Get All Food Success", A configured AllTripFood object.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodCorrectObject() throws Exception {
        // Arrange
        AllTripFood allTripFood = new AllTripFood();
        String tripId = "1234";
        String startStation = "Tokio";
        String endStation = "Osaka";

        List<TrainFood> trainFoodList = createSampleTrainFood();
        Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        Route route = new Route();
        List<String> ids = new ArrayList<>();
        ids.add("13");
        ids.add("21");
        route.setStations(ids);
        Response<Route> responseRoute = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        String startStationId = "13";
        Response<String> responseStartStation = new Response<>(1, "Success", startStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + startStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseStartStation), MediaType.APPLICATION_JSON));

        String endStationId = "21";
        Response<String> responseEndStation = new Response<>(1, "Success", endStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + endStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseEndStation), MediaType.APPLICATION_JSON));

        FoodStore fs = new FoodStore();
        fs.setId(UUID.randomUUID());
        fs.setStationId("21");
        List<FoodStore> fsList = new ArrayList<>();
        fsList.add(fs);
        Response<List<FoodStore>> responseFoodStore = new Response<>(1, "Success", fsList);
        uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/foodstores").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseFoodStore), MediaType.APPLICATION_JSON));

        Map<String, List<FoodStore>> foodStoreListMap = new HashMap<>();
        foodStoreListMap.put("13", new ArrayList<>());
        foodStoreListMap.put(fs.getStationId(), fsList);
        allTripFood.setTrainFoodList(trainFoodList);
        allTripFood.setFoodStoreListMap(foodStoreListMap);

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get All Food Success")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AlLTripFood object
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();
        Response<AllTripFood> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
        assertEquals(allTripFood, response.getData());
        assertEquals(trainFoodList, response.getData().getTrainFoodList());
        assertEquals(foodStoreListMap, response.getData().getFoodStoreListMap());

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more variables in the URL than it expects
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-food-map-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some arbitrary strings for the date, startStation, endStation, and tripId, and some additional random UUIDs.
     * The input only affects the behavior of the called services, which are mocked.
     * <li><b>Expected result:</b></li> status 1, msg "Get All Food Success", A configured AllTripFood object. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodMultipleVariables() throws Exception {
        // Arrange
        AllTripFood allTripFood = new AllTripFood();
        String tripId = "1234";
        String startStation = "Tokio";
        String endStation = "Osaka";

        List<TrainFood> trainFoodList = createSampleTrainFood();
        Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        Route route = new Route();
        List<String> ids = new ArrayList<>();
        ids.add("13");
        ids.add("21");
        route.setStations(ids);
        Response<Route> responseRoute = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        String startStationId = "13";
        Response<String> responseStartStation = new Response<>(1, "Success", startStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + startStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseStartStation), MediaType.APPLICATION_JSON));

        String endStationId = "21";
        Response<String> responseEndStation = new Response<>(1, "Success", endStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + endStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseEndStation), MediaType.APPLICATION_JSON));

        FoodStore fs = new FoodStore();
        fs.setId(UUID.randomUUID());
        fs.setStationId("21");
        List<FoodStore> fsList = new ArrayList<>();
        fsList.add(fs);
        Response<List<FoodStore>> responseFoodStore = new Response<>(1, "Success", fsList);
        uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/foodstores").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseFoodStore), MediaType.APPLICATION_JSON));

        Map<String, List<FoodStore>> foodStoreListMap = new HashMap<>();
        foodStoreListMap.put("13", new ArrayList<>());
        foodStoreListMap.put(fs.getStationId(), fsList);
        allTripFood.setTrainFoodList(trainFoodList);
        allTripFood.setFoodStoreListMap(foodStoreListMap);

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", startStation, endStation, tripId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get All Food Success")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AlLTripFood object
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();
        Response<AllTripFood> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
        assertEquals(allTripFood, response.getData());
        assertEquals(trainFoodList, response.getData().getTrainFoodList());
        assertEquals(foodStoreListMap, response.getData().getFoodStoreListMap());

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are no train foods associated with the given trip ID, meaning
     * the food map service returns a response with no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-food-map-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some arbitrary strings for the date, startStation, endStation, and tripId.
     * The input only affects the behavior of the called services, which are mocked.
     * <li><b>Expected result:</b></li> status 0, msg "Get the Food Request Failed", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodNoTrainFoodsForTripId() throws Exception {
        // Arrange
        Response<List<TrainFood>> responseTrainFood = new Response<>(0, "No content", null);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + "tripId").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        // Act
        mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), "tripId")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Get the Get Food Request Failed!")))
                .andExpect(jsonPath("$.data", is(nullValue())));

        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there are no food stores in the database, meaning
     * the food map service returns a response with no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-food-map-service</li>
     *   <li>ts-travel-service</li>
     *   <li>ts-station-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some arbitrary strings for the date, startStation, endStation, and tripId.
     * The input only affects the behavior of the called services, which are mocked.
     * <li><b>Expected result:</b></li> status 0, msg "Get All Food Failed", an empty AllTripFood object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodNoFoodStoresInDatabase() throws  Exception {
        // Arrange
        String tripId = "1234";
        String startStation = "Tokio";
        String endStation = "Osaka";

        List<TrainFood> trainFoodList = createSampleTrainFood();
        Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        Route route = new Route();
        List<String> ids = new ArrayList<>();
        ids.add("13");
        ids.add("21");
        route.setStations(ids);
        Response<Route> responseRoute = new Response<>(1, "Success", route);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + tripId).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        String startStationId = "13";
        Response<String> responseStartStation = new Response<>(1, "Success", startStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + startStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseStartStation), MediaType.APPLICATION_JSON));

        String endStationId = "21";
        Response<String> responseEndStation = new Response<>(1, "Success", endStationId);
        uri = UriComponentsBuilder.fromUriString("http://ts-station-service:12345/api/v1/stationservice/stations/id/" + endStation).build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseEndStation), MediaType.APPLICATION_JSON));

        Response<List<FoodStore>> responseFoodStore = new Response<>(0, "No content", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/foodstores").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseFoodStore), MediaType.APPLICATION_JSON));

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", UUID.randomUUID().toString(), startStation, endStation, tripId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Get All Food Failed")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AlLTripFood object
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();
        Response<AllTripFood> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
        assertEquals(new AllTripFood(), response.getData());
        mockServer.verify();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when when there are no route associated with the given trip ID,
     * meaning the travel service returns a response with no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-food-map-service</li>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> random UUIDs for the date, startStation, endStation, and tripId.
     * The input only affects the behavior of the called services, which are mocked.
     * <li><b>Expected result:</b></li> status 0, msg "Get All Food Failed", an empty AllTripFood object
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodNoRouteForTravelService() throws Exception {
        // Arrange
        List<TrainFood> trainFoodList = createSampleTrainFood();
        Response<List<TrainFood>> responseTrainFood = new Response<>(1, "Success", trainFoodList);
        URI uri = UriComponentsBuilder.fromUriString("http://ts-food-map-service:18855/api/v1/foodmapservice/trainfoods/" + "tripId").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseTrainFood), MediaType.APPLICATION_JSON));

        Response<Route> responseRoute = new Response<>(0, "No content", null);
        uri = UriComponentsBuilder.fromUriString("http://ts-travel-service:12346/api/v1/travelservice/routes/" + "tripId").build().toUri();

        mockServer.expect(ExpectedCount.once(), requestTo(uri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(responseRoute), MediaType.APPLICATION_JSON));

        // Act
        String result = mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}", "date", "startStation", "endStation", "tripId")
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Get All Food Failed")))
                .andReturn().getResponse().getContentAsString();

        // Check the data of the response to have the correct AlLTripFood object
        TypeFactory typeFactory = new ObjectMapper().getTypeFactory();
        Response<AllTripFood> response = new ObjectMapper().readValue(result, typeFactory.constructParametricType(Response.class, AllTripFood.class));
        assertEquals(new AllTripFood(), response.getData());
        mockServer.verify();
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
    public void testGetAllFoodMissingVariable() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the tripId is not suitable
     * <li><b>Parameters:</b></li> a tripId that is <= 2 characters long, and random UUIDs for the other parameters
     * <li><b>Expected result:</b></li> status 0, msg "Trip id is not suitable", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllFoodTripIdNotSuitable() throws Exception {
        // Arrange
        String tripId = "Id";

        // Act
        mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}",UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), tripId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Trip id is not suitable")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetAllFoodMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/foodservice/foods/{date}/{startStation}/{endStation}/{tripId}",UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    private List<TrainFood> createSampleTrainFood() {
        TrainFood tf = new TrainFood();
        tf.setId(UUID.randomUUID());
        List<TrainFood> list = new ArrayList<>();
        list.add(tf);
        return list;
    }
}
