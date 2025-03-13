package auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import auth.entity.User;

/**
 * @author fdse
 */
public interface UserRepository extends MongoRepository<User, String>
{
    /**
     * find by username
     *
     * @param username username
     * @return Optional<User>
     */
    Optional<User> findByUsername(String username);

    /**
     * delete by user id
     *
     * @param userId user id
     * @return null
     */
    void deleteByUserId(UUID userId);
}
