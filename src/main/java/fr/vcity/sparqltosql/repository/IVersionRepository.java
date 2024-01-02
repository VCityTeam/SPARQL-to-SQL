package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.Version;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IVersionRepository extends CrudRepository<Version, Integer> {

    @Query("""
    INSERT INTO version VALUES (DEFAULT, :message, :startTransactionTime, DEFAULT)
    RETURNING *
    """)
    Version save(String message, LocalDateTime startTransactionTime);

    @Query("""
    UPDATE version SET transaction_time_end = :endTransactionTime WHERE index_version = :indexVersion
    RETURNING *
    """)
    Version insertEndTransactionTime(Integer indexVersion, LocalDateTime endTransactionTime);
}
