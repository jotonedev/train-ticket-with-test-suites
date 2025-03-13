package cancel.component.get;

import cancel.entity.Order;
import cancel.entity.User;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CancelOrderTest {

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
     * <li><b>Parameters:</b></li> Some random orderId and loginId that will not be found by any of the order services
     * <li><b>Expected result:</b></li> status 0, msg "Order Not Found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderNoOrderFound() throws Exception {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String loginId = UUID.randomUUID().toString();

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
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Not Found.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass valid data where all services return a successful response.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service and some loginId
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceAllServicesSuccessful() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        User user = generateRandomUser(order.getAccountId());
        String userId = order.getAccountId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(1, "Draw Back Money Success", null);
        Response<User> userResponse = new Response<>(1, "Find User Success", user);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/id/" + userId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<User>>>any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-notification-service:17853/api/v1/notifyservice/notification/order_cancel_success"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Boolean>) ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(true));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service and some loginId and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderMultipleId() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        User user = generateRandomUser(order.getAccountId());
        String userId = order.getAccountId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(1, "Draw Back Money Success", null);
        Response<User> userResponse = new Response<>(1, "Find User Success", user);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/id/" + userId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<User>>>any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-notification-service:17853/api/v1/notifyservice/notification/order_cancel_success"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Boolean>) ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(true));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}", orderId, loginId, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    public void testCancelOrderMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testCancelOrderMalformedURL() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/{orderId}/{loginId}",UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the user-service fails to find a user
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service and some loginId
     * <li><b>Expected result:</b></li> status 0, msg "Cann't find userinfo by user id.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceNoUserFound() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String userId = order.getAccountId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(1, "Draw Back Money Success", null);
        Response<User> userResponse = new Response<>(0, "Find User Failure", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/id/" + userId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<User>>>any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Cann't find userinfo by user id.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the inside-payment service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service and some loginId
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceInsidePaymentFailure() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(0, "Draw Back Money Failure", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found that does not allow cancellations
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service that doesn't allow cancellations and some loginId
     * <li><b>Expected result:</b></li> status 0, msg "Failure", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceCancelNotAllowed() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(0, "Failure", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Failure")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the found order has a status that is not permitted for cancellations
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-service with a status that is neither PAID,
     * NOTPAID or CHANGE and some loginId
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceStatusNotPermitted() throws Exception {
        // Arrange
        Order order = generateRandomOrder(5, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(1, "Success", order);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Order Status Cancel Not Permitted")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass valid data where all services return a successful response.
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   <li>ts-user-service</li>
     *   <li>ts-notification-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-other-service and some loginId
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderOtherServiceAllServicesSuccessful() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(1, "Draw Back Money Success", null);

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

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the inside-payment service fails
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   <li>ts-inside-payment-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-other-service and some loginId
     * <li><b>Expected result:</b></li> status 1, msg "Success.", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderOtherServiceInsidePaymentFailure() throws Exception {
        // Arrange
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(1, "Success", order);
        Response<Object> paymentResponse = new Response<>(0, "Draw Back Money Failure", null);

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

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.<Class<Response>>any()))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success.")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when an order is found that does not allow cancellations
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-other-service that doesn't allow cancellations and some loginId
     * <li><b>Expected result:</b></li> status 0, msg "Failure", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderOtherServiceCancelNotAllowed() throws Exception {
        // Arrange
        String msgCancel = "Cancel failed";
        Order order = generateRandomOrder(1, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success", order);
        Response<Order> orderCancelResponse = new Response<>(0, msgCancel, null);

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

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderCancelResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Fail.Reason:" + msgCancel)))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the found order has a status that is not permitted for cancellations
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> And orderId that can be found by the order-other-service with a status that is neither PAID,
     * NOTPAID or CHANGE and some loginId
     * <li><b>Expected result:</b></li> status 0, msg "Order Status Cancel Not Permitted", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderOtherServiceStatusNotPermitted() throws Exception {
        // Arrange
        Order order = generateRandomOrder(5, false);
        String orderId = order.getId().toString();
        String loginId = UUID.randomUUID().toString();

        Response<Order> orderResponse = new Response<>(0, "Order Not Found", null);
        Response<Order> orderOtherResponse = new Response<>(1, "Success", order);

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
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId + "/" + loginId))
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
     * <li><b>Parameters:</b></li> a random orderId and loginId
     * <li><b>Expected result:</b></li> no content, as the controller catches the exception and returns null
     * </ul>
     * @throws Exception
     */
    @Test
    public void testCancelOrderViaOrderServiceSuccessButNull() throws Exception {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String loginId = UUID.randomUUID().toString();
        Response<Order> orderResponse = new Response<>(1, "Success.", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + orderId),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<Order>>>any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/cancelservice/cancel/" + orderId +"/" + loginId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(""));
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

    private User generateRandomUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(UUID.randomUUID().toString());
        user.setUserName(UUID.randomUUID().toString());
        return user;
    }
}
