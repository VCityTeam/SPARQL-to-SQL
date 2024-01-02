package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("versioned_named_graph")
public class RDFVersionedNamedGraph {
    @Id
    @Schema(name = "Versioned Named Graph id", example = "2")
    private Integer idVersionedNamedGraph;

    @Schema(name = "Named Graph ID", example = "1")
    private Integer idNamedGraph;

    @Schema(name = "Index", example = "2")
    private Integer index;

    public RDFVersionedNamedGraph(Integer idNamedGraph, Integer index, Integer idVersionedNamedGraph) {
        this.idNamedGraph = idNamedGraph;
        this.index = index;
        this.idVersionedNamedGraph = idVersionedNamedGraph;
    }
}
