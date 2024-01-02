package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("workspace")
public class Workspace {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Property ID", example = "2")
    private Integer idProperty;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    public Workspace(Integer idSubject, Integer idProperty, Integer idObject) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
    }

    public Workspace() {}

}
