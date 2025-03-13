package contacts.component.get;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetContactsByContactIdTest {

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
     * <li><b>Tests:</b></li> how the endpoint behaves when we try to get a Contact that does exist in the database
     * <li><b>Parameters:</b></li> A contactID that matches the ID of a Contact in the database
     * <li><b>Expected result:</b></li> status 1, msg "Success", the found contact.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetContactsByContactIdCorrectObject() throws Exception {
        // Arrange
        List<Contacts> contacts = createSampleContacts();
        Contacts contact = contacts.get(0);
        contactsRepository.save(contact);

        // Act
        mockMvc.perform(get("/api/v1/contactservice/contacts/{Id}", contact.getId())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(contact.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(contact.getAccountId().toString())))
                .andExpect(jsonPath("$.data.name", is(contact.getName())))
                .andExpect(jsonPath("$.data.documentNumber", is(contact.getDocumentNumber())))
                .andExpect(jsonPath("$.data.phoneNumber", is(contact.getPhoneNumber())))
                .andExpect(jsonPath("$.data.documentType", is(contact.getDocumentType())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one contactId as the URL parameter
     * <li><b>Parameters:</b></li> an element that matches the contactId given in paths and additional random strings
     * <li><b>Expected result:</b></li> status 1, msg "Success", the found contact. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetContactsByContactIdCorrectObjectMultipleId() throws Exception {
        // Arrange
        List<Contacts> contacts = createSampleContacts();
        Contacts contact = contacts.get(0);
        contactsRepository.save(contact);

        // Act
        mockMvc.perform(get("/api/v1/contactservice/contacts/{Id}", contact.getId(), UUID.randomUUID())
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data.id", is(contact.getId().toString())))
                .andExpect(jsonPath("$.data.accountId", is(contact.getAccountId().toString())))
                .andExpect(jsonPath("$.data.name", is(contact.getName())))
                .andExpect(jsonPath("$.data.documentNumber", is(contact.getDocumentNumber())))
                .andExpect(jsonPath("$.data.phoneNumber", is(contact.getPhoneNumber())))
                .andExpect(jsonPath("$.data.documentType", is(contact.getDocumentType())));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when there is no contact associated with the given contact ID
     * <li><b>Parameters:</b></li> some random contactId that does not match the ID of a contact in the database
     * <li><b>Expected result:</b></li> status 1, msg "No contacts according to contacts id", no data
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetContactsByContactIdNotExists() throws Exception {
        // Arrange
        String contactId = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/contactservice/contacts/{Id}", contactId)
                        .header(HttpHeaders.ACCEPT, "application/json"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No contacts according to contacts id")))
                .andExpect(jsonPath("$.data", is(nullValue())));
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
    public void testGetContactsByContactIdMissingId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(delete("/api/v1/contactservice/contacts/{Id}"));
        });
    }

    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testGetContactsByContactIdMalformedURL() throws Exception {
        // Act
        mockMvc.perform(delete("/api/v1/contactservice/contacts/{Id}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
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
