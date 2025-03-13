package verifycode.component.get;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetImageCodeTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether this endpoint generates an image code each time it is called
     * <li><b>Parameters:</b></li> none, but a cookie is provided with a value
     * <li><b>Expected result:</b></li> status is 200 OK with a present header for a set cookie and a byte array
     * with the first two bytes representing JPEG magic numbers
     * </ul>
     * @throws Exception
     */
    @Test
    public void testGenerateImageCode() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        Cookie cookie = new Cookie("YsbCaptcha", "needsToBeReplaced");

        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/generate")
                        .headers(headers)
                        .cookie(cookie))
                // Assert
                .andExpect(status().isOk())

                // Check if the response content contains something. Usually, it would be better to check
                // if the response content is an image/jpeg, but for that, the content type should be set
                // in the controller method (response.setContentType(MediaType.IMAGE_JPEG_VALUE)), which is not
                // the case here. Otherwise, we could use the following:
                // .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().string(not(emptyString())))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        // Check if the byte array starts with JPEG magic numbers
        assertThat(content[0]).isEqualTo((byte) 0xFF);
        assertThat(content[1]).isEqualTo((byte) 0xD8);

        // Check if the Set-Cookie header is present and if the cookie is replaced
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("YsbCaptcha=");
        assertThat(setCookieHeader).doesNotContain("needsToBeReplaced");
    }

    /**
     * <ul>
     * <li><b>Tests:</b></li> whether this endpoint generates an image code when the provided cookie has no value
     * <li><b>Parameters:</b></li> none, but a cookie is provided with no value
     * <li><b>Expected result:</b></li> status is 200 OK with a present header for a set cookie and a byte array
     * with the first two bytes representing JPEG magic numbers
     * <li><b>Related Issue:</b></li> <b>F19:</b> The implementation of the service checks if the cookie's value is not null
     * with {@code if (cookie.getValue() != null)}. If this condition is true, it generates a new cookie ID and adds it to the
     * response. Conversely, if the cookie's value is null, it assigns the existing cookie's value (which is null) to cookieId. This logic
     * is flawed because it should generate a new cookieID when the cookie's value is null, not when it is not null. This will
     * lead to an unhandled exception being raised when the cookieId is put into the Cache<String, String> because the key is null.
     * </ul>
     * @throws Exception
     */
    @Test
    public void FAILING_testGenerateImageCodeCookieNoValue() throws Exception {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        Cookie cookie = new Cookie("YsbCaptcha", null);

        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/verifycode/generate")
                        .headers(headers)
                        .cookie(cookie))
                // Assert
                .andExpect(status().isOk())

                // Check if the response content contains something. Usually, it would be better to check
                // if the response content is an image/jpeg, but for that, the content type should be set
                // in the controller method (response.setContentType(MediaType.IMAGE_JPEG_VALUE)), which is not
                // the case here. Otherwise, we could use the following:
                // .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().string(not(emptyString())))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        // Check if the byte array starts with JPEG magic numbers
        assertThat(content[0]).isEqualTo((byte) 0xFF);
        assertThat(content[1]).isEqualTo((byte) 0xD8);

        // Check if the Set-Cookie header is present and if the cookie is replaced
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("YsbCaptcha=");
    }
}
