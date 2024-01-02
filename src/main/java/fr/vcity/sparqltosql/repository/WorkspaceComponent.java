package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.Workspace;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceComponent {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public WorkspaceComponent(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Workspace save(
            Integer idSubject,
            Integer idProperty,
            Integer idObject
    ) {
        return namedParameterJdbcTemplate.queryForObject("""
                        INSERT INTO workspace (id_subject, id_property, id_object)
                        VALUES (:idSubject, :idProperty, :idObject)
                        RETURNING *;
                        """,
                new MapSqlParameterSource()
                        .addValue("idSubject", idSubject)
                        .addValue("idProperty", idProperty)
                        .addValue("idObject", idObject),
                (rs, i) -> new Workspace(
                        rs.getInt("id_subject"),
                        rs.getInt("id_property"),
                        rs.getInt("id_object")
                )
        );
    }
}
