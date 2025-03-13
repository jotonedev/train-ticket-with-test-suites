package price.component.get;

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
import price.entity.PriceConfig;
import price.repository.PriceConfigRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FindAllPriceConfigTest {

    @Autowired
    private PriceConfigRepository priceConfigRepository;

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
    void beforeEach() {
        priceConfigRepository.deleteAll();
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> that all stored PriceConfigs are returned on endpoint call
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 1, msg "Success", Array of all PriceConfig objects
     * @throws Exception
     */
    @Test
    public void testFindAllPriceConfigElementsInDatabase() throws Exception {
        // Arrange
        for (int i = 0; i < 1000; i++) {
            PriceConfig priceConfig = new PriceConfig();
            priceConfig.setId(UUID.randomUUID());
            priceConfigRepository.save(priceConfig);
        }

        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(1)))
                .andExpect(jsonPath("$.msg", is("Success")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1000)));
    }


    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the database is empty
     * <li><b>Parameters:</b></li> none
     * <li><b>Expected result:</b></li> status 0, msg "No price config", empty list of PriceConfig objects
     * </ul>
     * @throws Exception
     */
    @Test
    public void testFindAllPriceConfigEmptyDatabase() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/priceservice/prices"))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(0)))
                .andExpect(jsonPath("$.msg", is("No price config")))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
