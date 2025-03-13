package adminbasic.component.put;

import adminbasic.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ModifyConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when config service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random Config Object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the config service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsConfigServiceSuccessful() throws Exception {
        // Arrange
        Config expectedConfig = generateRandomConfig();
        Response<Config> configResponse = new Response<>(1, "Update success", expectedConfig);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-config-service:15679/api/v1/configservice/configs"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.<HttpEntity<Config>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(configResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedConfig)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Update success")))
                .andExpect(jsonPath("$.data.name", is(expectedConfig.getName())))
                .andExpect(jsonPath("$.data.description", is(expectedConfig.getDescription())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Config objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsMultipleObjects() throws Exception {
        // Arrange
        List<Config> configList = new ArrayList<>();
        configList.add(new Config());
        configList.add(new Config());

        String jsonRequest = new ObjectMapper().writeValueAsString(configList);

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the input JSON is malformed in any way (too many attributes, wrong attributes etc.)
     * <li><b>Parameters:</b></li> a malformed JSON object
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give nothing to the endpoint
     * <li><b>Parameters:</b></li> empty requestJson
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the request body is null
     * <li><b>Parameters:</b></li> null request body
     * <li><b>Expected result:</b></li> an error response indicating that the request body is missing
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-config-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random config object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the config service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyConfigsConfigServiceCrash() throws Exception {
        // Arrange
        Config expectedConfig = generateRandomConfig();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-config-service:15679/api/v1/configservice/configs"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedConfig)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Config generateRandomConfig() {
        Config config = new Config();
        config.setName(UUID.randomUUID().toString());
        config.setDescription(UUID.randomUUID().toString());
        return config;
    }
}
