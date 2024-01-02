package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Scenario {
    @Schema(
            name = "The scenario name",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_2018_Workspace#scenario_1"
    )
    String scenarioName;

    @Schema(
            name = "The associated versions"
    )
    List<String> versions;

    public Scenario(String scenarioName, List<String> versions) {
        this.scenarioName = scenarioName;
        this.versions = versions;
    }
}
