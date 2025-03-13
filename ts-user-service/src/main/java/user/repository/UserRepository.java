package user.repository;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import user.entity.User;

/**
 * @author fdse
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>
{
    User findByUserName(String userName);

    User findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
