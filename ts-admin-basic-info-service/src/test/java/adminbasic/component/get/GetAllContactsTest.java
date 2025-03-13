package adminbasic.component.get;

import adminbasic.entity.*;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllContactsTest {

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
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the contacts service to the client, which in this case
     * is a successful response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllContactsContactServiceSuccessful() throws Exception {
        // Arrange
        ArrayList<Contacts> contactList = new ArrayList<>();
        Contacts expectedContacts = generateRandomContacts();
        contactList.add(expectedContacts);
        int expectedContactsListLength = contactList.size();
        Response<ArrayList<Contacts>> contactResponse = new Response<>(1, "Success", contactList);

        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-contacts-service:12347/api/v1/contactservice/contacts"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(contactResponse, HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminbasicservice/adminbasic/contacts"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedContactsListLength)))
                .andExpect(jsonPath("$.data[0].id", is(expectedContacts.getId().toString())))
                .andExpect(jsonPath("$.data[0].name", is(expectedContacts.getName())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether the endpoint can handle null responses by the other services
     * <li><b>Mocked:</b></li>
     *   <ul>
     *   <li>ts-contacts-service</li>
     *   </ul>
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> The endpoint passes the response from the contacts service to the client, which in this case
     * is a fallback response
     * <li><b>Related Issue:</b></li> <b>S1:</b> The issue with the fallback response is that its message to the user
     * is meaningless. Therefore, it is unknown whether the developer knew of the occurrence of that specific error or not.
     * is the fallback response
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllContactsContactServiceCrash() throws Exception {
        // Arrange
        when(restTemplate.exchange(
                ArgumentMatchers.contains("http://ts-contacts-service:12347/api/v1/contactservice/contacts"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.any(HttpEntity.class),
                (Class<Response>) ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(new Response<>(null, null, null), HttpStatus.OK));

        // Act
        mockMvc.perform(get("/api/v1/adminbasicservice/adminbasic/contacts"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", nullValue()))
                .andExpect(jsonPath("$.msg", nullValue()))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    private Contacts generateRandomContacts() {
        Contacts contacts = new Contacts();
        contacts.setId(UUID.randomUUID());
        contacts.setName(UUID.randomUUID().toString());
        return contacts;
    }
}
