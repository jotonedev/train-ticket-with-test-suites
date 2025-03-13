package verifycode.component.get;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import verifycode.service.impl.VerifyCodeServiceImpl;

import javax.servlet.http.Cookie;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VerifyCodeTest {

    // Needed to configure the cacheCode in testVerifyCodeValidCode()
    @Autowired
    private VerifyCodeServiceImpl verifyCodeService;

    @Autowired
    private MockMvc mockMvc;

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the verifyCode given in paths matches the code stored in the cache for the
     * given session
     * <li><b>Parameters:</b></li> a valid (meaning it is stored in the cache) verifyCode
     * <li><b>Expected result:</b></li> boolean value true for valid verifyCode
     * </ul>
     * @throws Exception
     */
    @Test
    public void testVerifyCodeValidCode() throws Exception {
        // Arrange
        String ysbCaptcha = "YsbCaptcha";

        String verifyCode = UUID.randomUUID().toString();
        String cookieId = UUID.randomUUID().toString();

        // Add the valid code to the cache
        verifyCodeService.cacheCode.put(cookieId, verifyCode);

        // Create a new cookie with the cookieId
        Cookie cookie = new Cookie(ysbCaptcha, cookieId);

        // Act
        mockMvc.perform(get("/api/v1/verifycode/verify/{verifyCode}", verifyCode)
                        .cookie(cookie)) // Add the cookie to the request
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when the verifyCode given in paths does not match the code stored
     * in the cache for the given session
     * <li><b>Parameters:</b></li> an invalid (meaning it is not stored in the cache) verifyCode
     * <li><b>Expected result:</b></li> boolean value false for invalid verifyCode
     * </ul>
     * @throws Exception
     */
    @Test
    public void testVerifyCodeInvalidCode() throws Exception {
        // Arrange
        String verifyCode = UUID.randomUUID().toString();

        // Act
        mockMvc.perform(get("/api/v1/verifycode/verify/{verifyCode}", verifyCode))
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> how the endpoint behaves when we give the endpoint request more than one code as the URL parameter
     * <li><b>Parameters:</b></li> a valid (meaning it is stored in the cache) verifyCode and an additional random string
     * <li><b>Expected result:</b></li> boolean value true for valid verifyCode. Only the first needed values are used.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testVerifyCodeMultipleId() throws Exception {
        // Arrange
        String ysbCaptcha = "YsbCaptcha";

        String verifyCode = UUID.randomUUID().toString();
        String cookieId = UUID.randomUUID().toString();

        // Add the valid code to the cache
        verifyCodeService.cacheCode.put(cookieId, verifyCode);

        // Create a new cookie with the cookieId
        Cookie cookie = new Cookie(ysbCaptcha, cookieId);

        // Act
        mockMvc.perform(get("/api/v1/verifycode/verify/{verifyCode}", verifyCode, UUID.randomUUID().toString())
                        .cookie(cookie)) // Add the cookie to the request
                .andDo(print())
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
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
    public void testVerifyCodeMissingCode() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            mockMvc.perform(get("/api/v1/verifycode/verify/{verifyCode}"));
        });
    }


    /**
     * <li><b>Tests:</b></li> how the endpoint behaves when the URL parameter is malformed in any way
     * <li><b>Parameters:</b></li> a malformed URL parameter
     * <li><b>Expected result:</b></li> a 4xx client error.
     * @throws Exception
     */
    @Test
    public void testVerifyCodeMalformedCode() throws Exception {
        // Act
        mockMvc.perform(get("/api/v1/verifycode/verify/{verifyCode}",UUID.randomUUID() + "/" + UUID.randomUUID()))
                // Assert
                .andExpect(status().is4xxClientError());
    }

}
