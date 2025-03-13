package fdse.microservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import fdse.microservice.entity.Station;

public interface StationRepository extends CrudRepository<Station, String>
{
    Station findByName(String name);

    @Override
    Optional<Station> findById(String id);

    @Override
    List<Station> findAll();
}
