package auth.service;

import org.springframework.http.HttpHeaders;

import auth.dto.BasicAuthDto;
import edu.fudan.common.util.Response;

/**
 * @author fdse
 */
public interface TokenService
{
    /**
     * get token by dto
     *
     * @param dto dto
     * @param headers headers
     * @return Response
     */
    Response getToken(BasicAuthDto dto, HttpHeaders headers);
}
