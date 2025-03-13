package cancel.component.get;

import cancel.entity.Order;
import edu.fudan.common.util.Response;
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
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CalculateRefundTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when both order services do not find an order
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> Some random orderId that will not be found by any of the order services
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderNotFound() throws Exception {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-service which has a status of NOTPAID
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of NOTPAID
     * <li><b>Expected result:</b></li> status 1, msg "Success Refoud 0", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderNotPaidOrderServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(0, false);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. Refoud 0")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-other-service which has a status of NOTPAID
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of NOTPAID
     * <li><b>Expected result:</b></li> status 1, msg "Success Refoud 0", "0"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderNotPaidOrderOtherServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(0, false);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success, Refound 0")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-service which has a status of
     * PAID and its travelTime is in the future
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of PAID with a travelTime in the future
     * <li><b>Expected result:</b></li> status 1, msg "Success. ", "80,00"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderPaidInFutureOrderServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, true);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. ")))
                .andExpect(jsonPath("$.data", is("80,00")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an element that matches the name given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", "80,00". Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundMultipleId() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, true);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}", orderId, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. ")))
                .andExpect(jsonPath("$.data", is("80,00")));
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
    public void testQueryStationIdForNameMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testQueryStationIdForNameMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/{orderId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-other-service which has a status of
     * PAID and its travelTime is in the future
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of PAID with a travelTime in the future
     * <li><b>Expected result:</b></li> status 1, msg "Success. ", "80,00"
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderPaidInFutureOrderOtherServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, true);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Arrange
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is("80,00")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-service which has a status of
     * PAID and its travelTime is in the past
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of PAID with a travelTime in the future
     * <li><b>Expected result:</b></li> status 1, msg "Success. ", "0"
     * <li><b>Related Issue:</b></li> <b>F10:</b> The ts-cancel-service initializes a Date object by passing a year,
     * month, day, hour, minute, and second. However, in all methods of class Date that accept a year, the year y is represented
     * by the integer y - 1900, which was not taken into account here. Therefore, the nowDate will always be before the startTime
     * and therefore always return price * 0.8, leading to "80,00" always being returned, although this shouldn't be the case.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCalculateRefundOrderPaidFromPastOrderServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success. ")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-other-service which has a status of
     * PAID and its travelTime is in the past
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of PAID with a travelTime in the future
     * <li><b>Expected result:</b></li> status 1, msg "Success. ", "0"
     * <li><b>Related Issue:</b></li> <b>F10:</b> The ts-cancel-service initializes a Date object by passing a year,
     * month, day, hour, minute, and second. However, in all methods of class Date that accept a year, the year y is represented
     * by the integer y - 1900, which was not taken into account here. Therefore, the nowDate will always be before the startTime
     * and therefore always return price * 0.8, leading to "80,00" always being returned, although this shouldn't be the case.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testCalculateRefundOrderPaidFromPastOrderOtherServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data", is("0")));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-service which has a status of
     * neither PAID nor UNPAID
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of neither PAID nor UNPAID
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted, Refound error. ", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderNotPaidOrUnpaidOrderServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(2, true);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Status Cancel Not Permitted, Refound error")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found by the order-other-service which has a status of
     * neither PAID nor UNPAID
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-order-other-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> an orderId finding an order that has a status of neither PAID nor UNPAID
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderNotPaidOrUnpaidOrderOtherServiceSuccess() throws Exception {
        // Arrange
        Order order = generateRandomOrder(2, true);
        String orderId = order.getId().toString();
        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success.", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderOtherResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Status Cancel Not Permitted")))
                .andExpect(jsonPath("$.data", nullValue()));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the order service succeeds, but returns null
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> a random orderId
     * <li><b>Expected result:</b></li> fallback response on error: 200 OK, status message and data are null.
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCalculateRefundOrderServiceSuccessButNull() throws Exception {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/refound/" + orderId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Order generateRandomOrder(int status, boolean inFuture) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(UUID.randomUUID());
        order.setStatus(status);
        Date date;
        if (inFuture) {
            date = new Date(System.currentTimeMillis() + 1000000000);
        }
        else {
            date = new Date(System.currentTimeMillis() - 1000000000);
        }
        order.setTravelDate(date);
        order.setTravelTime(date);
        order.setPrice("100.00");
        return order;
    }
}
