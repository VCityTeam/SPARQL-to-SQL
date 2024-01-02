package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScenarioVersion {

    @Schema(
            name = "The space type",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Ontologies/Workspace/3.0/workspace#ConcensusSpace"
    )
    private String spaceType;

    @Schema(
            name = "The Scenario name",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_2018_Workspace#scenario_1"
    )
    private String scenario;

    @Schema(
            name = "The previous version of the current version",
            example = "null"
    )
    private String previousVersion;

    @Schema(
            name = "The current version",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_split#version_2009"
    )
    private String version;

    @Schema(
            name = "The next version of the current version",
            example = "https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2012_split#version_2012"
    )
    private String nextVersion;

}
