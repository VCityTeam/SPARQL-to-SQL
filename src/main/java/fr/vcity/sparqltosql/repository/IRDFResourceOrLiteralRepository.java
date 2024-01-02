package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRDFResourceOrLiteralRepository extends CrudRepository<RDFResourceOrLiteral, Integer> {

    @Query("""
    INSERT INTO resource_or_literal VALUES (DEFAULT, :name, :type)
    ON CONFLICT (sha512(name::bytea), type) DO UPDATE SET type = EXCLUDED.type
    RETURNING *
    """)
    RDFResourceOrLiteral save(String name, String type);
}
