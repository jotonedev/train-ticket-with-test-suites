package auth.security.jwt;

import java.util.Base64;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import auth.constant.InfoConstant;
import auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * @author fdse
 */
@Component
public class JWTProvider
{
    private String secretKey = "secret";

    private long validityInMilliseconds = 3600000;

    public String createToken(User user)
    {

        Claims claims = Jwts.claims().setSubject(user.getUsername());
        claims.put(InfoConstant.ROLES, user.getRoles());
        claims.put(InfoConstant.ID, user.getUserId());

        Date now = new Date();
        Date validate = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validate)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    @PostConstruct
    protected void init()
    {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }
}