package fr.vcity.sparqltosql.controllers;

import fr.vcity.sparqltosql.services.QuadImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.riot.RiotException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Tag(name = "Import API")
@RequestMapping("/import")
public class QuadImportController {
    QuadImportService quadImportService;

    public QuadImportController(QuadImportService quadImportService) {
        this.quadImportService = quadImportService;
    }

    @Operation(
            summary = "Adds quads and creates a new version",
            description = "Adds all quads as a new version and considered as valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The quads were added to a new version",
                    content = {
                            @Content(mediaType = "text/plain",
                                    schema = @Schema(implementation = Integer.class)
                            )}),
            @ApiResponse(responseCode = "400", description = "Invalid content")}
    )
    @PostMapping(value = {"/version"})
    ResponseEntity<Integer> importModel(
            @Parameter(description = "The file containing all the triple/quads to import as valid in a new version", name = "file")
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Integer indexVersion = quadImportService.importModel(file);
            return ResponseEntity.ok(indexVersion);
        } catch (RiotException e) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }
    }

    @Operation(
            summary = "Adds triple and creates a new workspace",
            description = "Adds all triple as a new workspace and considered as valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The triple were added to a new workspace"),
            @ApiResponse(responseCode = "400", description = "Invalid content")}
    )
    @PostMapping(value = {"/workspace"})
    ResponseEntity<Void> importWorkspace(
            @Parameter(description = "The file containing all the triple to import as valid in a new workspace", name = "file")
            @RequestParam("file") MultipartFile file
    ) {
        try {
            quadImportService.importWorkspace(file);
            return ResponseEntity.ok().build();
        } catch (RiotException e) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }
    }

    @Operation(
            summary = "Remove the current workspace",
            description = "Make the current workspace as null"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The workspace have been cleaned")
    })
    @DeleteMapping(value = {"/workspace"})
    void removeWorkspace() {
        quadImportService.removeWorkspace();
    }
}
