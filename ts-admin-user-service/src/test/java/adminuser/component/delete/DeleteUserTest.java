package adminuser.component.delete;

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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteUserTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when user service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random userId
     * <li><b>Expected result:</b></li> The endpoint passes the response from the user service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserUserServiceSuccessful() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Response<Object> userResponse = new Response<>(1, "DELETE SUCCESS", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/" + userId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminuserservice/users/{userId}", userId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random userId
     * <li><b>Expected result:</b></li> The endpoint passes the response from the user service to the client, which in this case
     * is a successful response. Only the first id is used in the request to the User service
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserMultipleIds() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Response<Object> userResponse = new Response<>(1, "DELETE SUCCESS", null);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/" + userId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminuserservice/users/{userId}", userId, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("DELETE SUCCESS")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random userId
     * <li><b>Expected result:</b></li> The endpoint passes the response from the user service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteUserUserServiceCrash() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users/" + userId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminuserservice/users/{userId}", userId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
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
    public void testDeleteUserMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/adminuserservice/users/{userId}"));
        });
    }


    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testDeleteUserMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/adminuserservice/users/{userId}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }
}
