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
public class ModifyTrainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when train service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random TrainType Object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the train service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyTrainsTrainServiceSuccessful() throws Exception {
        // Arrange
        TrainType expectedTrainType = generateRandomTrainType();
        boolean expectedSuccess = true;
        Response<Boolean> TrainResponse = new Response<>(1, "Modify success", expectedSuccess);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.<HttpEntity<TrainType>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(TrainResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTrainType)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data", is(expectedSuccess)));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two TrainType objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyTrainsMultipleObjects() throws Exception {
        // Arrange
        List<TrainType> trainTypeList = new ArrayList<>();
        trainTypeList.add(new TrainType());
        trainTypeList.add(new TrainType());

        String jsonRequest = new ObjectMapper().writeValueAsString(trainTypeList);

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
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
    public void testModifyTrainsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
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
    public void testModifyTrainsMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
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
    public void testModifyTrainsNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
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
     *   <li>ts-train-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random TrainType Object
     * <li><b>Expected result:</b></li> The endpoint passes the response from the train service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyTrainsTrainServiceCrash() throws Exception {
        // Arrange
        TrainType expectedTrainType = generateRandomTrainType();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-train-service:14567/api/v1/trainservice/trains"),
                ArgumentMatchers.eq(HttpMethod.PUT),
                ArgumentMatchers.<HttpEntity<Station>>any(),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(null);

        // Act
        mockMvc.perform(put("/api/v1/adminbasicservice/adminbasic/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(expectedTrainType)))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private TrainType generateRandomTrainType() {
        TrainType trainType = new TrainType();
        trainType.setId(UUID.randomUUID().toString());
        return trainType;
    }
}
