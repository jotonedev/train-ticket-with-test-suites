package plan.component.auth;

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
import plan.entity.RoutePlanInfo;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchCheapestResultAuthMissingAuthorization() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchCheapestResultAuthUser() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchCheapestResultAuthAdmin() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/cheapestRoute")
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchMinStopStationsAuthMissingAuthorization() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchMinStopStationsAuthUser() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchMinStopStationsAuthAdmin() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/minStopStations")
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchQuickestResultAuthMissingAuthorization() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info)))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchQuickestResultAuthUser() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service are permitted,
     * so the status should be OK when no headers are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPostSearchQuickestResultAuthAdmin() throws Exception {
        // Assert
        RoutePlanInfo info = new RoutePlanInfo();

        // Act
        mockMvc.perform(post("/api/v1/routeplanservice/routePlan/quickestRoute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(info))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }
}
