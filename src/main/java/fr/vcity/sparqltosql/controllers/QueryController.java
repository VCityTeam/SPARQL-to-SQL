package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.VersionAncestry;
import fr.vcity.sparqltosql.dto.Workspace;
import fr.vcity.sparqltosql.services.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Tag(name = "Query API")
@RequestMapping("/query")
public class QueryController {
    QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(
            summary = "Search by validity",
            description = "Search all quads filtered by a given validity and returns the result"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {
                            @Content(mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = RDFCompleteVersionedQuad.class)
                                    )
                            )}),
            @ApiResponse(responseCode = "400", description = "Invalid validity",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @GetMapping(value = "/validity/{pattern}")
    ResponseEntity<List<RDFCompleteVersionedQuad>> queryRequestedValidity(
            @Parameter(description = "The validity string (in bit string format)", name = "pattern", example = "110")
            @PathVariable("pattern") String requestedValidity
    ) {
        return ResponseEntity.ok(queryService.queryRequestedValidity(requestedValidity));
    }

    @Operation(
            summary = "Search by version",
            description = "Find all quads filtered by a given version and returns the result"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = RDFCompleteVersionedQuad.class)
                            )
                    )}),
            @ApiResponse(responseCode = "400", description = "Invalid version",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @GetMapping("/version/{idVersion}")
    ResponseEntity<List<RDFCompleteVersionedQuad>> queryRequestedVersion(
            @Parameter(description = "The version number", name = "idVersion", example = "3")
            @PathVariable("idVersion") Integer requestedVersion
    ) {
        return ResponseEntity.ok(queryService.queryRequestedVersion(requestedVersion));
    }

    @Operation(
            summary = "SPARQL query endpoint",
            description = "Executes the SPARQL query and returns the result"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The query filtered result",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = RDFCompleteVersionedQuad.class)
                            )
                    )}),
            @ApiResponse(responseCode = "400", description = "Invalid SPARQL request",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Nothing found",
                    content = @Content)}
    )
    @PostMapping("/sparql")
    ResponseEntity<List<RDFCompleteVersionedQuad>> querySPARQL(
            @RequestBody(description = "The SPARQL query", required = true)
            @org.springframework.web.bind.annotation.RequestBody String queryString
    ) {
        return ResponseEntity.ok(queryService.querySPARQL(queryString));
    }

    @Operation(
            summary = "Get version tree",
            description = "Gets all versions and their ancestry"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The version tree",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = VersionAncestry.class)
                            )
                    )})}
    )
    @GetMapping("/versions")
    ResponseEntity<Workspace> getGraphVersion() {
        return ResponseEntity.ok(queryService.getGraphVersion());
    }
}
