package fr.vcity.sparqltosql.dao;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("resource_or_literal")
public class RDFResourceOrLiteral {
    @Id
    @Schema(name = "Resource or literal ID", example = "1")
    private Integer idResourceOrLiteral;

    @Schema(name = "Name of the resource or literal", example = "100")
    private String name;

    @Schema(name = "Type of the literal", example = "http://www.w3.org/2001/XMLSchema#string", description = "NOT NULL if literal")
    private String type;
}
