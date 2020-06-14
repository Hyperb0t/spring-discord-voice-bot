package ru.itis.springbot.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.itis.springbot.models.Record;

import java.util.Optional;

@Repository
public interface RecordRepository extends CrudRepository<Record, Long> {
    Optional<Record> findByTextContains(String s);
}
