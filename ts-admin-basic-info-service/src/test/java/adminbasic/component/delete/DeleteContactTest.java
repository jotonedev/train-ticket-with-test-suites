package adminbasic.component.delete;

import adminbasic.entity.*;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeleteContactTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when contacts service returns a successful response
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-contacts-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random contactsId
     * <li><b>Expected result:</b></li> The endpoint passes the response from the contacts service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteContactsContactServiceSuccessful() throws Exception {
        // Arrange
        UUID expectedContactId = UUID.randomUUID();
        Response<UUID> contactResponse = new Response<>(1, "Delete success", expectedContactId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + expectedContactId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(contactResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", expectedContactId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data", is(expectedContactId.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one id as the URL parameter
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-contacts-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> multiple random contactsIds
     * <li><b>Expected result:</b></li> The endpoint passes the response from the contacts service to the client, which in this case
     * is a successful response. Only the first values are considered, and the rest are ignored.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteContactsMultipleIds() throws Exception {
        // Arrange
        UUID expectedContactId = UUID.randomUUID();
        Response<UUID> contactResponse = new Response<>(1, "Delete success", expectedContactId);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + expectedContactId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(contactResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", expectedContactId, UUID.randomUUID().toString()))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Delete success")))
                .andExpect(jsonPath("$.data", is(expectedContactId.toString())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteContactsMalformedId() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}", UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
    public void testDeleteContactsMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/adminbasicservice/adminbasic/contacts/{contactsId}"));
        });
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-contacts-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> some random contactsId
     * <li><b>Expected result:</b></li> The endpoint passes the response from the contacts service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testDeleteContactsContactServiceCrash() throws Exception {
        // Arrange
        UUID expectedContactId = UUID.randomUUID();

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-contacts-service:12347/api/v1/contactservice/contacts/" + expectedContactId),
                ArgumentMatchers.eq(HttpMethod.DELETE),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(delete("/api/v1/adminbasicservice/adminbasic/contacts/" + expectedContactId))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }
}
