package auth.service.impl;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import auth.constant.InfoConstant;
import auth.dto.BasicAuthDto;
import auth.dto.TokenDto;
import auth.entity.User;
import auth.exception.UserOperationException;
import auth.repository.UserRepository;
import auth.security.jwt.JWTProvider;
import auth.service.TokenService;
import edu.fudan.common.util.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fdse
 */
@Service
@Slf4j
public class TokenServiceImpl implements TokenService
{
    @Autowired
    private JWTProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.verification.code.service.url:ts-verification-code-service}")
    private String tsVerificationCodeServiceUrl;

    @Value("${ts.verification.code.service.port:15678}")
    private String tsVerificationCodeServicePort;


    @Override
    public Response getToken(BasicAuthDto dto, HttpHeaders headers)
    {
        String username = dto.getUsername();
        String password = dto.getPassword();
        String verifyCode = dto.getVerificationCode();
        log.info("LOGIN USER :" + username + " __ " + password + " __ " + verifyCode);

        if (!StringUtils.isEmpty(verifyCode)) {
            HttpEntity requestEntity = new HttpEntity(headers);
            ResponseEntity<Boolean> re = restTemplate.exchange(
                "http://"+tsVerificationCodeServiceUrl+":"+tsVerificationCodeServicePort+"/api/v1/verifycode/verify/" + verifyCode,
                HttpMethod.GET,
                requestEntity,
                Boolean.class);
            boolean id = re.getBody();

            // failed code
            if (!id) {
                return new Response<>(0, "Verification failed.", null);
            }
        }

        // verify username and password
        UsernamePasswordAuthenticationToken upat = new UsernamePasswordAuthenticationToken(username, password);
        try {
            authenticationManager.authenticate(upat);
        } catch (AuthenticationException e) {
            return new Response<>(0, "Incorrect username or password.", null);
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserOperationException(MessageFormat.format(
                InfoConstant.USER_NAME_NOT_FOUND_1, username
            )));
        String token = jwtProvider.createToken(user);
        log.info(token + "USER TOKEN");
        log.info(user.getUserId() + "   USER ID");
        return new Response<>(1, "login success", new TokenDto(user.getUserId(), username, token));
    }
}
