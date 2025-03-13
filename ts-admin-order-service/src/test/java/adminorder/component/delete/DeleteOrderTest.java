package adminorder.component.delete;

import adminorder.entity.Order;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteOrderTest {

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
     * <li><b>Parameters:</b></li> orderId and train number that starts with G of the to be deleted order
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderTrainNumberStartsWithGOrderServiceSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        UUID expectedOrderId = expectedOrder.getId();
        String expectedTrainNumber = "G" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Delete Order Success", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Order Success")))
                .andExpect(jsonPath("$.data.id", is(expectedOrder.getId().toString())))
                .andExpect(jsonPath("$.data.trainNumber", is(expectedTrainNumber)))
                .andExpect(jsonPath("$.data.contactsName", is(expectedOrder.getContactsName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-travel-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> tripId that starts with G of the to be deleted trip, and a random UUID
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is a successful response. Only the first values are considered, and the rest are ignored.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderMultipleIds() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        UUID expectedOrderId = expectedOrder.getId();
        String expectedTrainNumber = "G" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Delete Order Success", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber, UUID.randomUUID(), UUID.randomUUID()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Order Success")))
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
     * <li><b>Parameters:</b></li> orderId and train number that starts with D of the to be deleted order
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderTrainNumberStartsWithDOrderServiceSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        UUID expectedOrderId = expectedOrder.getId();
        String expectedTrainNumber = "D" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Delete Order Success", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Order Success")))
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
     * <li><b>Parameters:</b></li> orderId and train number that neither starts with G nor D of the to be deleted order
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order-other service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderTrainNumberNotStartsWithDOrGOrderOtherSuccessful() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        UUID expectedOrderId = expectedOrder.getId();
        String expectedTrainNumber = "A" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        Response<Order> orderResponse = new Response<>(1, "Delete Order Success", expectedOrder);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(orderResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete Order Success")))
                .andExpect(jsonPath("$.data.id", is(expectedOrder.getId().toString())))
                .andExpect(jsonPath("$.data.trainNumber", is(expectedTrainNumber)))
                .andExpect(jsonPath("$.data.contactsName", is(expectedOrder.getContactsName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses the other requested endpoints
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-order-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> orderId and train number that starts with G or D of the to be deleted order
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderTrainNumberStartsWithGOrDOrderServiceCrash() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "D" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        UUID expectedOrderId = expectedOrder.getId();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-service:12031/api/v1/orderservice/order/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));
        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber))
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
     * <li><b>Parameters:</b></li> orderId and train number that neither starts with G nor D of the to be deleted order
     * <li><b>Expected result:</b></li> The endpoint passes the response from the order-other service to the client, which in this case
     * is the fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteTrainNumberNotStartsWithGOrDOrderOtherServiceCrash() throws Exception {
        // Arrange
        Order expectedOrder = generateRandomOrder();
        String expectedTrainNumber = "A" + UUID.randomUUID();
        expectedOrder.setTrainNumber(expectedTrainNumber);
        UUID expectedOrderId = expectedOrder.getId();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-order-other-service:12032/api/v1/orderOtherService/orderOther/" + expectedOrderId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminorderservice/adminorder/" + expectedOrderId + "/" + expectedTrainNumber))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderMalformedId() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/admintravelservice/adminorder/{orderId}/{trainNumber}", UUID.randomUUID() + "/" + UUID.randomUUID(), UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testDeleteOrderMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/admintravelservice/adminorder/{orderId}/{trainNumber}"));
        });
    }

    private Order generateRandomOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setContactsName(UUID.randomUUID().toString());
        return order;
    }
}
