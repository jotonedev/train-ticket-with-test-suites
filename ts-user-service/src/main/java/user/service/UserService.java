package user.service;

import java.util.UUID;

import org.springframework.http.HttpHeaders;

import edu.fudan.common.util.Response;
import user.dto.UserDto;

/**
 * @author fdse
 */
public interface UserService
{
    Response saveUser(UserDto user, HttpHeaders headers);

    Response getAllUsers(HttpHeaders headers);

    Response findByUserName(String userName, HttpHeaders headers);

    Response findByUserId(String userId, HttpHeaders headers);

    Response deleteUser(UUID userId, HttpHeaders headers);

    Response updateUser(UserDto user, HttpHeaders headers);
}
