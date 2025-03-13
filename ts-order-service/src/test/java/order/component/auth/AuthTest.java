package order.component.auth;

import order.entity.Order;
import order.entity.OrderInfo;
import order.entity.OrderStatus;
import order.entity.Seat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AuthTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F26:</b> The SecurityConfig of the ts-order-service specified the endpoint
     * to only be accessible by user with user or admin role. However, the url is not correct, it should be /api/v1/orderservice/order/{orderId}
     * or /api/v1/orderservice/order/**, instead of /api/v1/orderservice/order. Therefore the restriction is not applied to the endpoint.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteOrderAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be Ok, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteOrderAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * @throws Exception
     */
    @Test
    public void testGetCheckSecurityAboutOrderAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/security/{checkDate}/{accountId}", date, input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetCheckSecurityAboutOrderAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/security/{checkDate}/{accountId}", date, input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetCheckSecurityAboutOrderAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/security/{checkDate}/{accountId}", date, input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersAuthMissingAuthorization() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/orderservice/order"))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersAuthUser() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllOrdersAuthAdmin() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetOrderByIdAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetOrderByIdAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetOrderByIdAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * @throws Exception
     */
    @Test
    public void testGetOrderPriceAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/price/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetOrderPriceAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/price/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetOrderPriceAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/price/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * @throws Exception
     */
    @Test
    public void testGetModifyOrderAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        int code = OrderStatus.PAID.getCode();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/status/{orderId}/{status}", input, code)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetModifyOrderAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        int code = OrderStatus.PAID.getCode();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/status/{orderId}/{status}", input, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetModifyOrderAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        int code = OrderStatus.PAID.getCode();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/status/{orderId}/{status}", input, code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetPayOrderAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/orderPay/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetPayOrderAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/orderPay/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetPayOrderAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/orderPay/{orderId}", input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQueryAlreadySoldOrdersAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", date, input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with a user token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQueryAlreadySoldOrdersAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", date, input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQueryAlreadySoldOrdersAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();
        Date date = new Date("Sat Jul 29 00:00:00 GMT+0800 2017");

        // Act
        mockMvc.perform(get("/api/v1/orderservice/order/{travelDate}/{trainNumber}", date, input)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPostCreateAdminOrderAuthMissingAuthorization() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostCreateAdminOrderAuthUser() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request, and alter the
     * URL by adding a trailing slash to the endpoint
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role. Even when the URL
     * contains a trailing slash, the endpoint should still be protected.
     * <li><b>Related Issue:</b></li> <b>F12:</b> The protected endpoint /api/v1/orderservice/order/admin is accessible
     * without the required role by appending a trailing slash to the URL. The endpoint should be protected even when the URL
     * is altered.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPostCreateAdminOrderAlteredURL() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/admin" + "/") // Altered URL
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostCreateAdminOrderAuthAdmin() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPostCreateOrderAuthMissingAuthorization() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(order)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostCreateOrderAuthUser() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostCreateOrderAuthAdmin() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostGetSoldTicketsAuthMissingAuthorization() throws Exception {
        // Assert
        Seat seat = new Seat();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(seat)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostGetSoldTicketsAuthUser() throws Exception {
        // Assert
        Seat seat = new Seat();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(seat))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostGetSoldTicketsAuthAdmin() throws Exception {
        // Assert
        Seat seat = new Seat();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(seat))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersForRefreshAuthMissingAuthorization() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersForRefreshAuthUser() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersForRefreshAuthAdmin() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when no headers are passed
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersAuthMissingAuthorization() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersAuthUser() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostQueryOrdersAuthAdmin() throws Exception {
        // Assert
        OrderInfo info = new OrderInfo();
        info.setLoginId(UUID.randomUUID().toString());

        // Act
        mockMvc.perform(post("/api/v1/orderservice/order/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPutUpdateAdminOrderAuthMissingAuthorization() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPutUpdateAdminOrderAuthUser() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request, and alter the
     * URL by adding a trailing slash to the endpoint
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role. Even when the URL
     * contains a trailing slash, the endpoint should still be protected.
     * <li><b>Related Issue:</b></li> <b>F12:</b> The protected endpoint /api/v1/orderservice/order/admin is accessible
     * without the required role by appending a trailing slash to the URL. The endpoint should be protected even when the URL
     * is altered.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPutUpdateAdminOrderAlteredURL() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order/admin" + "/") // Altered URL
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().isForbidden());
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPutUpdateAdminOrderAuthAdmin() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPutUpdateOrderAuthMissingAuthorization() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPutUpdateOrderAuthUser() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires either user or admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPutUpdateOrderAuthAdmin() throws Exception {
        // Assert
        Order order = new Order();

        // Act
        mockMvc.perform(put("/api/v1/orderservice/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }
}
