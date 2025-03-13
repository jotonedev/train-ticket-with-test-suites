package adminroute.component.auth;

import adminroute.entity.RouteInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.testcontainers.utility.DockerImageName;

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
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteRouteAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/adminrouteservice/adminroute/{routeId}", input))
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
    public void testDeleteRouteAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/adminrouteservice/adminroute/{routeId}", input)
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
    public void testDeleteRouteAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/adminrouteservice/adminroute/{routeId}", input)
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass no headers to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F22:</b> The service returns a status 403 Forbidden instead of 401 Unauthorized.
     * Status 403 should be returned when Authorization is present, but the role of the user is not sufficient for the request.
     * However, since there is no authorization header present, the status should be 401 Unauthorized.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGetAllRoutesAuthMissingAuthorization() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/adminrouteservice/adminroute"))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllRoutesAuthUser() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/adminrouteservice/adminroute")
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().isForbidden());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllRoutesAuthAdmin() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/adminrouteservice/adminroute")
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
    public void FAILING_testPostAddRouteAuthMissingAuthorization() throws Exception {
        // Assert
        RouteInfo routeInfo = new RouteInfo();

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeInfo)))
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
    public void testPostAddRouteAuthUser() throws Exception {
        // Assert
        RouteInfo routeInfo = new RouteInfo();

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeInfo))
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
    public void testPostAddRouteAuthAdmin() throws Exception {
        // Assert
        RouteInfo routeInfo = new RouteInfo();

        // Act
        mockMvc.perform(post("/api/v1/adminrouteservice/adminroute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(routeInfo))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }
}
