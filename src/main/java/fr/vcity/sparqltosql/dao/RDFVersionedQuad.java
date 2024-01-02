package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_quad")
public class RDFVersionedQuad {

    @Schema(name = "Subject ID", example = "1")
    private Integer idSubject;

    @Schema(name = "Property ID", example = "2")
    private Integer idProperty;

    @Schema(name = "Object ID", example = "3")
    private Integer idObject;

    @Schema(name = "Named Graph ID", example = "4")
    private Integer idNamedGraph;

    @Schema(name = "Validity", example = "B'10001'")
    private byte[] validity;

    public RDFVersionedQuad(Integer idSubject, Integer idProperty, Integer idObject, Integer idNamedGraph, byte[] validity) {
        this.idSubject = idSubject;
        this.idProperty = idProperty;
        this.idObject = idObject;
        this.idNamedGraph = idNamedGraph;
        this.validity = validity;
    }
}
