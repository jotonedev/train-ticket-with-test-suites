package adminuser.component.get;

import adminuser.entity.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllUsersTest {

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
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the user service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllUsersUserServiceSuccessful() throws Exception {
        // Arrange
        List<User> userList = new ArrayList<>();
        User expectedUser = generateRandomUser();
        userList.add(expectedUser);
        int expectedUserListLength = userList.size();
        Response<List<User>> userResponse = new Response<>(1, "Success", userList);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<List<User>>>>any()))
                .thenReturn(new ResponseEntity<>(userResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminuserservice/users"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedUserListLength)))
                .andExpect(jsonPath("$.data[0].userId", is(expectedUser.getUserId().toString())))
                .andExpect(jsonPath("$.data[0].userName", is(expectedUser.getUserName())))
                .andExpect(jsonPath("$.data[0].email", is(expectedUser.getEmail())));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-user-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the user service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllUsersUserServiceCrash() throws Exception {
        // Arrange
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-user-service:12342/api/v1/userservice/users"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Response<List<User>>>>any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminuserservice/users"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private User generateRandomUser() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(UUID.randomUUID().toString());
        user.setEmail(UUID.randomUUID().toString());
        return user;
    }
}
