package assurance.component.get;

import assurance.entity.AssuranceType;
import assurance.entity.AssuranceTypeBean;
import assurance.repository.AssuranceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetAllAssuranceTypeTest {

    @Autowired
    private AssuranceRepository assuranceRepository;

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
        assuranceRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that all stored AssuranceTypeBean objects are returned on endpoint call
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Find All Assurance", list of AssuranceTypeBean. The AssuranceType are
     * already predefined in the enum AssuranceType. it will therefore only return one single element within a list (the definition
     * of the types beforehand is also the reason why there is no test for the case where the list is empty, as you would have to
     * adjust the implementation of the code for that, which would change its behavior).
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGetAllAssuranceTypeExistingAssuranceTypes() throws Exception {
        // Arrange
        int expectedAmountAssuranceTypes = AssuranceType.values().length;
        assertEquals(1, expectedAmountAssuranceTypes);

        AssuranceTypeBean expectedAssuranceTypeBean = new AssuranceTypeBean();
        expectedAssuranceTypeBean.setName("Traffic Accident Assurance");
        expectedAssuranceTypeBean.setPrice(3.0);
        expectedAssuranceTypeBean.setIndex(1);

        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/types"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Find All Assurance")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(expectedAmountAssuranceTypes)))
                .andExpect(jsonPath("$.data[0].index", is(expectedAssuranceTypeBean.getIndex())))
                .andExpect(jsonPath("$.data[0].name", is(expectedAssuranceTypeBean.getName())))
                .andExpect(jsonPath("$.data[0].price", is(expectedAssuranceTypeBean.getPrice())));
    }

    /*
    If we were able to change alter the predefined AssuranceType enum, we could add a test for the case where the list is empty.
    This would be the implementation of the test:
     */
    /*
    @Test
    public void testGetAllAssuranceTypeNoAssuranceTypes() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/assuranceservice/assurances/types"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("Assurance is Empty")))
                .andExpect(jsonPath("$.data", is(nullValue())));
    }
     */
}
