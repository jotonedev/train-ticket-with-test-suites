package config.component.auth;

import config.entity.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenAdmin;
import static edu.fudan.common.security.jwt.JWTGenerator.generateJwtTokenUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteConfigAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", input)
                        .contentType(MediaType.APPLICATION_JSON))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteConfigAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", input)
                        .contentType(MediaType.APPLICATION_JSON)
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
    public void testDeleteConfigAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/configservice/configs/{configName}", input)
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
    public void testGetQueryAllAuthMissingAuthorization() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/configservice/configs"))
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
    public void testGetQueryAllAuthUser() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/configservice/configs")
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request`
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> SecurityConfig has no specific requirements for this endpoint, so the status
     * should be OK when headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetQueryAllAuthAdmin() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/configservice/configs")
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
    public void testGetRetrieveConfigAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", input)
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
    public void testGetRetrieveConfigAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", input)
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
     * should be OK headers with an admin token are passed
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetRetrieveConfigAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/configservice/configs/{configName}", input)
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
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPostCreateConfigAuthMissingAuthorization() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPostCreateConfigAuthUser() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config))
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
    public void testPostCreateConfigAuthAdmin() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(post("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config))
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
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPutUpdateConfigAuthMissingAuthorization() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config)))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies that this endpoint requires admin role, so the status
     * should be Forbidden, as authorization is specified, but the user does not have the required role.
     * <li><b>Related Issue:</b></li> <b>F15a:</b> The SecurityConfig of the ts-config-service specifies that
     * POST PUT and DELETE endpoints require admin role. However, because {@code .antMatchers("/api/v1/configservice/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testPutUpdateConfigAuthUser() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config))
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
    public void testPutUpdateConfigAuthAdmin() throws Exception {
        // Assert
        Config config = new Config();

        // Act
        mockMvc.perform(put("/api/v1/configservice/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(config))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }
}
