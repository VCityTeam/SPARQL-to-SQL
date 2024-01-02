package fr.vcity.sparqltosql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VersionAncestry {

    @Schema(name = "The index of the version", example = "3")
    private Integer indexVersion;

    @Schema(name = "The parent index", example = "1")
    private Integer indexParent;

    @Schema(name = "The ancestry of the version", example = "{null,1}")
    private List<Integer> ancestry;

    public VersionAncestry(Integer indexVersion, Integer indexParent, List<Integer> ancestry) {
        this.indexVersion = indexVersion;
        this.indexParent = indexParent;
        this.ancestry = ancestry;
    }
}
