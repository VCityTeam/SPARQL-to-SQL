package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Space {
    @Schema(
            name = "The space type",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#ConcensusSpace"
    )
    String spaceType;

    @Schema(
            name = "The associated scenarios"
    )
    List<Scenario> scenarios;

    public Space(String spaceType, List<Scenario> scenarios) {
        this.spaceType = spaceType;
        this.scenarios = scenarios;
    }
}
