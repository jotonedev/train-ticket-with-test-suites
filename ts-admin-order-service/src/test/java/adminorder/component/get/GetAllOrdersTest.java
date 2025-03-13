package adminorder.component.get;

import adminorder.entity.Order;
import edu.fudan.common.util.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllOrdersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order and orderOther service return no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Get the orders successfully!", an empty array list
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersOrderAndOrderOtherNoContent() throws Exception {
        // Arrange
        Response<ArrayList<Order>> orderResponse = new Response<>(0, "No Content.", null);
        Response<ArrayList<Order>> orderOtherResponse = new Response<>(0, "No Content", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminorderservice/adminorder"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get the orders successfully!")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order returns a list of orders and orderOther service returns no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Get the orders successfully!", the combined list of orders from both services
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersOrderOtherNoContent() throws Exception {
        // Arrange
        ArrayList<Order> orderList = generateRandomOrderList(3);
        ArrayList<Order> orderOtherList = new ArrayList<>();
        List<UUID> expectedTotalOrderIdsList = extractUUIDListFromTwoOrderArrays(orderList, orderOtherList);

        Response<ArrayList<Order>> orderResponse = new Response<>(1, "Success.", orderList);
        Response<ArrayList<Order>> orderOtherResponse = new Response<>(0, "No Content", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/adminorderservice/adminorder"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get the orders successfully!")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalOrderIdsList.size())))
                .andReturn();

        List<UUID> responseIds = extractUUIDListFromMVCResult(result);
        assertThat(expectedTotalOrderIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the orderOther returns a list of orders and order service returns no content
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Get the orders successfully!", the combined list orders from both services
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersOrderNoContent() throws Exception {
        // Arrange
        ArrayList<Order> expectedOrderList = new ArrayList<>();
        ArrayList<Order> expectedOrderOtherList = generateRandomOrderList(3);
        List<UUID> expectedTotalOrderIdsList = extractUUIDListFromTwoOrderArrays(expectedOrderList, expectedOrderOtherList);

        Response<ArrayList<Order>> orderResponse = new Response<>(0, "No Content", null);
        Response<ArrayList<Order>> orderOtherResponse = new Response<>(1, "Success", expectedOrderOtherList);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/adminorderservice/adminorder"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get the orders successfully!")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalOrderIdsList.size())))
                .andReturn();

        List<UUID> responseIds = extractUUIDListFromMVCResult(result);
        assertThat(expectedTotalOrderIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order and orderOther return a list of orders
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Get the orders successfully!", the combined list of orders from both services
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersBothServicesNotEmpty() throws Exception {
        // Arrange
        ArrayList<Order> expectedOrderList = generateRandomOrderList(5);
        ArrayList<Order> expectedOrderOtherList = generateRandomOrderList(3);
        List<UUID> expectedTotalOrderIdsList = extractUUIDListFromTwoOrderArrays(expectedOrderList, expectedOrderOtherList);

        Response<ArrayList<Order>> orderResponse = new Response<>(1, "Success.", expectedOrderList);
        Response<ArrayList<Order>> orderOtherResponse = new Response<>(1, "Success", expectedOrderOtherList);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v1/adminorderservice/adminorder"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Get the orders successfully!")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedTotalOrderIdsList.size())))
                .andReturn();

        List<UUID> responseIds = extractUUIDListFromMVCResult(result);
        assertThat(expectedTotalOrderIdsList).containsExactlyInAnyOrderElementsOf(responseIds);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order service crashes and triggers the fallback response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the orderOther service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersOrderServiceCrash() throws Exception {
        // Arrange
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<ArrayList<Order>>>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminorderservice/adminorder"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private List<UUID> extractUUIDListFromMVCResult(MvcResult result) throws JSONException, UnsupportedEncodingException {
        JSONObject responseJson = new JSONObject(result.getResponse().getContentAsString());
        JSONArray responseArray =  responseJson.getJSONArray("data");
        ArrayList<UUID> responseIds = new ArrayList<>();
        for (int i = 0; i < responseArray.length(); i++) {
            responseIds.add(UUID.fromString(responseArray.getJSONObject(i).getString("id")));
        }
        return responseIds;
    }

    private List<UUID> extractUUIDListFromTwoOrderArrays(List<Order> orderList, List<Order> orderOtherList) {
        ArrayList<Order> expectedTotalOrderList = new ArrayList<>();
        expectedTotalOrderList.addAll(orderList);
        expectedTotalOrderList.addAll(orderOtherList);

        return expectedTotalOrderList.stream()
                .map(Order::getId).collect(Collectors.toList());
    }

    private ArrayList<Order> generateRandomOrderList(int length) {
        ArrayList<Order> orderList = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            orderList.add(generateRandomOrder());
        }

        return orderList;
    }

    private Order generateRandomOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setContactsName(UUID.randomUUID().toString());
        return order;
    }
}
