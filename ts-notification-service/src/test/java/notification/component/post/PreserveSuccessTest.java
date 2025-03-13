package notification.component.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import notification.entity.NotifyInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PreserveSuccessTest {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * <ul>
     * <li><b>Tests:</b></li> that the endpoint for sending an email works correctly, for a valid notification information.
     * <li><b>Parameters:</b></li> a valid NotifyInfo object
     * <li><b>Expected result:</b></li> boolean value true for valid notification information
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveSuccessCorrectObject() throws Exception {
        // Arrange
        NotifyInfo info = createSampleNotifyInfo();

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        String result = mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertTrue(Boolean.parseBoolean(result));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that the endpoint for sending an email works correctly, for an empty notification information.
     * <li><b>Parameters:</b></li> an empty NotifyInfo object
     * <li><b>Expected result:</b></li> boolean value false for invalid notification information
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveSuccessEmptyObject() throws Exception {
        // Arrange
        NotifyInfo info = new NotifyInfo();

        String jsonRequest = new ObjectMapper().writeValueAsString(info);

        // Act
        String result = mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertFalse(Boolean.parseBoolean(result));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two NotifyInfo objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testPreserveSuccessMultipleObjects() throws Exception {
        // Arrange
        NotifyInfo info1 = createSampleNotifyInfo();
        NotifyInfo info2 = createSampleNotifyInfo();
        NotifyInfo[] infos = {info1, info2};
        String jsonRequest = new ObjectMapper().writeValueAsString(infos);

        // Act
        mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
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
    public void testPreserveSuccessMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
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
    public void testPreserveSuccessMissingBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
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
    public void testPreserveSuccessNullRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(post("/api/v1/notifyservice/notification/preserve_success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }


    private NotifyInfo createSampleNotifyInfo() {
        NotifyInfo notifyInfo = new NotifyInfo();
        notifyInfo.setEmail("test@test.com");
        notifyInfo.setOrderNumber("1");
        notifyInfo.setUsername("user");
        notifyInfo.setStartingPlace("start");
        notifyInfo.setEndPlace("end");
        notifyInfo.setStartingTime("startTime");
        notifyInfo.setDate("date");
        notifyInfo.setSeatClass("seatClass");
        notifyInfo.setSeatNumber("1");
        notifyInfo.setPrice("1");
        return notifyInfo;
    }
}
