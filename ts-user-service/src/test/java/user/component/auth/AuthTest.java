package user.component.auth;

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
import user.entity.User;

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
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service require authorization (either user or admin),
     * so the status should be 401 Unauthorized when no headers are passed
     * <li><b>Related Issue:</b></li> <b>F15b:</b> The SecurityConfig of the ts-user-service specifies that the
     * DELETE endpoint requires admin or user role. However, because {@code .antMatchers("/api/v1/userservice/users/**").permitAll()}
     * is the first rule, all endpoints are accessible without authorization.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testDeleteUserByIdAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", input))
                // Assert
                .andExpect(status().isUnauthorized());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with a user token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service require authorization (either user or admin),
     * so the status should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", input)
                        .header("Authorization", "Bearer " + generateJwtTokenUser()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we pass headers with an admin token to the request
     * <li><b>Parameters:</b></li> input is irrelevant for testing the authorization
     * <li><b>Expected result:</b></li> SecurityConfig specifies all endpoints of the service require authorization (either user or admin),
     * so the status should be OK, as the user has the required role.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserByIdAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(delete("/api/v1/userservice/users/{userId}", input)
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
    public void testGetUserByUserIdAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", input))
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
    public void testGetUserByUserIdAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", input)
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
    public void testGetUserByUserIdAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/id/{userId}", input)
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
    public void testGetUserByUserNameAuthMissingAuthorization() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/{userName}", input))
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
    public void testGetUserByUserNameAuthUser() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/{userName}", input)
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
    public void testGetUserByUserNameAuthAdmin() throws Exception {
        // Assert
        String input = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/userservice/users/{userName}", input)
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
    public void testPostRegisterUserAuthMissingAuthorization() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
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
    public void testPostRegisterUserAuthUser() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
    public void testPostRegisterUserAuthAdmin() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(post("/api/v1/userservice/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
    public void testPutUpdateUserAuthMissingAuthorization() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
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
    public void testPutUpdateUserAuthUser() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
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
    public void testPutUpdateUserAuthAdmin() throws Exception {
        // Assert
        User user = new User();

        // Act
        mockMvc.perform(put("/api/v1/userservice/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user))
                        .header("Authorization", "Bearer " + generateJwtTokenAdmin()))
                // Assert
                .andExpect(status().is2xxSuccessful());
    }
}
