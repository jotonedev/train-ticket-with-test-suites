package contacts.component.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import contacts.entity.Contacts;
import contacts.entity.DocumentType;
import contacts.repository.ContactsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ModifyContactsTest {

    @Autowired
    private ContactsRepository contactsRepository;

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

    @BeforeEach
    public void beforeEach() {
        contactsRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give a valid contact object to the endpoint with an id that
     * corresponds to an existing contact record in the repository
     * <li><b>Parameters:</b></li> a contact object with valid values for all attributes and an id that exists in the repository.
     * <li><b>Expected result:</b></li> status 1, msg "Modify success", the updated contact object. The contact object in the repository is updated.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyContactsCorrectObject() throws Exception {
        // Arrange
        List<Contacts> contacts = createSampleContacts();
        Contacts contact = contacts.get(0);
        contactsRepository.save(contact);
        contact.setName("NewName");

        String jsonRequest = new ObjectMapper().writeValueAsString(contact);

        // Act
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Modify success")))
                .andExpect(jsonPath("$.data.id", is(contact.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(contact.getAccountId().toString())))
                .andExpect(jsonPath("$.data.name", is(contact.getName())))
                .andExpect(jsonPath("$.data.documentNumber", is(contact.getDocumentNumber())))
                .andExpect(jsonPath("$.data.phoneNumber", is(contact.getPhoneNumber())))
                .andExpect(jsonPath("$.data.documentType", is(contact.getDocumentType())));

        // Make sure that the contact was updated in the database
         Contacts modifiedContact = contactsRepository.findById(contact.getId());
        assertEquals(contact, modifiedContact);
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one object in the JSON.
     * <li><b>Parameters:</b></li> Two Contact objects with valid values for all attributes
     * <li><b>Expected result:</b></li> a 4xx client error.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyContactsMultipleObjects() throws Exception {
        // Arrange
        List<Contacts> contacts = createSampleContacts();

        String jsonRequest = new ObjectMapper().writeValueAsString(contacts.toArray());

        // Act
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().is4xxClientError());
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no element in the database that matches the id of the contact
     * <li><b>Parameters:</b></li> a contact with a random id that does not exist in the database
     * <li><b>Expected result:</b></li> status 0, msg "Contacts not found", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testModifyContactsMissingObject() throws Exception {
        // Arrange
        List<Contacts> contacts = createSampleContacts();
        Contacts contact = contacts.get(0);
        contact.setId(UUID.randomUUID());

        String jsonRequest = new ObjectMapper().writeValueAsString(contact);

        // Act
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Contacts not found")))
                .andExpect(jsonPath("$.data", nullValue()));
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
    public void testModifyContactsMalformedObject() throws Exception {
        // Arrange
        String requestJson = "{\"id\":wrong, \"name\":value, \"stayTime\":null}";

        // Act
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isBadRequest());
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
    public void testModifyContactsEmptyBody() throws Exception {
        // Arrange
        String requestJson = "";

        // Act
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(HttpHeaders.ACCEPT, "application/json"))
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
    public void testModifyContactsNulLRequestBody() throws Exception {
        // Act
        String expectedError = "Required request body is missing";
        mockMvc.perform(put("/api/v1/contactservice/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(null)))
                // Assert
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()))
                .andExpect(result -> assertTrue(Objects.requireNonNull(
                        result.getResolvedException()).getMessage().contains(expectedError)));
    }

    private List<Contacts> createSampleContacts() {
        List<Contacts> contacts = new ArrayList<>();
        Contacts contactsOne = new Contacts();
        contactsOne.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        contactsOne.setDocumentType(DocumentType.ID_CARD.getCode());
        contactsOne.setName("Contacts_One");
        contactsOne.setDocumentNumber("DocumentNumber_One");
        contactsOne.setPhoneNumber("ContactsPhoneNum_One");
        contactsOne.setId(UUID.fromString("4d2a46c7-71cb-4cf1-a5bb-b68406d9da6f"));
        contacts.add(contactsOne);

        Contacts contactsTwo = new Contacts();
        contactsTwo.setAccountId(UUID.fromString("4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"));
        contactsTwo.setDocumentType(DocumentType.ID_CARD.getCode());
        contactsTwo.setName("Contacts_Two");
        contactsTwo.setDocumentNumber("DocumentNumber_Two");
        contactsTwo.setPhoneNumber("ContactsPhoneNum_Two");
        contactsTwo.setId(UUID.fromString("4d2546c7-71cb-4cf1-a5bb-b68406d9da6f"));
        contacts.add(contactsTwo);
        return contacts;
    }
}
