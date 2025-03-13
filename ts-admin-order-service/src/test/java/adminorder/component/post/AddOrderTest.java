package adminorder.component.post;

import adminorder.entity.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

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
public class AddOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when order service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added Order object with a train number that starts with G
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderTrainNumberWithGOrderServiceSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "G" + UUID.randomUUID().toString();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Success.", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/admin"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<Order>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedOrder)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data.id", is(expectedOrder.getId().toString())))
                .andExpect(jsonPath("$.data.trainNumber", is(expectedTrainNumber)))
                .andExpect(jsonPath("$.data.contactsName", is(expectedOrder.getContactsName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when order service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added order object with a train number that starts with D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderTrainNumberWithDOrderServiceSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "D" + UUID.randomUUID().toString();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Success.", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/admin"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<Order>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedOrder)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data.id", is(expectedOrder.getId().toString())))
                .andExpect(jsonPath("$.data.trainNumber", is(expectedTrainNumber)))
                .andExpect(jsonPath("$.data.contactsName", is(expectedOrder.getContactsName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when order-other service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added order object with a train number that neither starts with G nor D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order-other service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderTrainNumberWithNotDOrGOrderOtherServiceSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "A" + UUID.randomUUID().toString();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Success", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/admin"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.<HttpEntity<Order>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedOrder)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(expectedOrder.getId().toString())))
                .andExpect(jsonPath("$.data.trainNumber", is(expectedTrainNumber)))
                .andExpect(jsonPath("$.data.contactsName", is(expectedOrder.getContactsName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Order objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderMultipleObjects() throws Exception {
        // Arrange
        List<Order> orderList = new ArrayList<>();
        orderList.add(new Order());
        orderList.add(new Order());

        String requestJson = new ObjectMapper().writeValueAsString(orderList);

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
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
    public void testAddOrderMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
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
    public void testAddOrderMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
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
    public void testAddOrderNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added order object with a train number that starts with G or D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderTrainNumberGOrDOrderServiceCrash() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "D" + UUID.randomUUID().toString();
        expectedOrder.setTrainNumber(expectedTrainNumber);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/admin"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedOrder)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> to be added order object with a train number that neither starts with G nor D
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order-other service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testAddOrderTrainNumberNotGOrDOrderOtherServiceCrash() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "A" + UUID.randomUUID().toString();
        expectedOrder.setTrainNumber(expectedTrainNumber);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/admin"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(post("/api/v1/adminorderservice/adminorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedOrder)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Order generateRandomOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setContactsName(UUID.randomUUID().toString());
        return order;
    }
}
