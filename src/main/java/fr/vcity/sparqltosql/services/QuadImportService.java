package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import fr.vcity.sparqltosql.dao.Version;
import fr.vcity.sparqltosql.model.RDFSavedTriple;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Iterator;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IVersionedWorkspaceRepository versionedWorkspaceRepository;
    IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository;
    RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent;
    WorkspaceComponent workspaceComponent;
    IVersionRepository versionRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QuadImportService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IVersionedWorkspaceRepository versionedWorkspaceRepository,
            IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository,
            RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent,
            WorkspaceComponent workspaceComponent,
            IVersionRepository versionRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.versionedWorkspaceRepository = versionedWorkspaceRepository;
        this.rdfVersionedNamedGraphRepository = rdfVersionedNamedGraphRepository;
        this.rdfVersionedNamedGraphComponent = rdfVersionedNamedGraphComponent;
        this.workspaceComponent = workspaceComponent;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
        this.versionRepository = versionRepository;
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new version.
     *
     * @param file The input file
     */
    @Override
    public Integer importModel(MultipartFile file) throws RiotException {
        LocalDateTime startTransactionTime = LocalDateTime.now();
        Version version = versionRepository.save(file.getOriginalFilename(), startTransactionTime);

        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Dataset dataset =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            Long start = System.nanoTime();

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                log.info("Name Graph : {}", namedModel.getURI());
                RDFResourceOrLiteral rdfResourceOrLiteralNG = saveRDFResourceOrLiteralOrReturnExisting(namedModel, "Named Graph");
                RDFResourceOrLiteral rdfResourceOrLiteralVNG = saveRDFResourceOrLiteralOrReturnExisting(
                        model.createResource(generateVersionedNamedGraph(namedModel.getURI(), version.getIndexVersion() - 1)),
                        "Named Graph"
                );
                saveRDFNamedGraphOrReturnExisting(
                        rdfResourceOrLiteralVNG.getIdResourceOrLiteral(),
                        version.getIndexVersion() - 1,
                        rdfResourceOrLiteralNG.getIdResourceOrLiteral()
                );

                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

                    rdfVersionedQuadComponent.save(
                            rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                            rdfResourceOrLiteralNG.getIdResourceOrLiteral(),
                            version.getIndexVersion() - 1
                    );
                });
            }

            Long end = System.nanoTime();
            log.info("Time of execution: {} ns for file: {}", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            versionRepository.delete(version);
            throw new RiotException(e.getMessage());
        }

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
        LocalDateTime endTransactionTime = LocalDateTime.now();

        versionRepository.insertEndTransactionTime(version.getIndexVersion(), endTransactionTime);
        return version.getIndexVersion();
    }

    /**
     * Deletes all the elements inside the database
     */
    @Override
    public void resetDatabase() {
        rdfVersionedQuadRepository.deleteAll();
        rdfResourceRepository.deleteAll();
        rdfVersionedNamedGraphRepository.deleteAll();
        versionRepository.deleteAll();
        versionedWorkspaceRepository.deleteAll();
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new workspace.
     *
     * @param file The input file
     */
    @Override
    public void importWorkspace(MultipartFile file) throws RiotException {

        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Dataset dataset =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            Long start = System.nanoTime();

            if (!dataset.getDefaultModel().listStatements().toList().isEmpty()) {
                importDefaultModel(dataset.getDefaultModel());
            }

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

                    workspaceComponent.save(
                            rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral()
                    );
                });
            }

            Long end = System.nanoTime();
            log.info("Time of execution: {} ns for file: {}", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            throw new RiotException(e.getMessage());
        }
    }

    /**
     * Reset the workspace.
     */
    public void removeWorkspace() {
        versionedWorkspaceRepository.deleteAll();
    }

    /**
     * Import RDF default model statements
     *
     * @param defaultModel The default graph
     */
    private void importDefaultModel(Model defaultModel) {
        defaultModel.listStatements().toList().parallelStream().forEach(statement -> {
            RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

            workspaceComponent.save(
                    rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                    rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                    rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral()
            );
        });
    }

    /**
     * Saves the subject, the property, the object and the named graph inside the database if they exist else returning them
     *
     * @param statement The statement
     * @return The saved or existing Quad
     */
    private RDFSavedTriple saveRDFTripleOrReturnExisting(Statement statement) {
        RDFNode subject = statement.getSubject();
        RDFNode predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        RDFResourceOrLiteral savedRDFSubject = saveRDFResourceOrLiteralOrReturnExisting(subject, "Subject");
        RDFResourceOrLiteral savedRDFPredicate = saveRDFResourceOrLiteralOrReturnExisting(predicate, "Predicate");
        RDFResourceOrLiteral savedRDFObject = saveRDFResourceOrLiteralOrReturnExisting(object, "Object");

        log.debug("Insert or updated (S: {}, P: {}, O: {})",
                savedRDFSubject.getName(),
                savedRDFPredicate.getName(),
                savedRDFObject.getName()
        );

        return new RDFSavedTriple(savedRDFSubject, savedRDFPredicate, savedRDFObject);
    }

    /**
     * Saves and return the RDF Named Graph inside the database if it doesn't exist, else returns the existing one.
     *
     * @param idVersionedNamedGraph The RDF versioned named graph id
     * @param index                 The number of the associated version
     * @param idNamedGraph          The RDF named graph URI id
     * @return The saved or existing RDFNamedGraph element
     */
    private RDFVersionedNamedGraph saveRDFNamedGraphOrReturnExisting(Integer idVersionedNamedGraph, Integer index, Integer idNamedGraph) {
        log.info("Upsert named graph: {} in version {}. versionedId : {}", idNamedGraph, index, idVersionedNamedGraph);
        return rdfVersionedNamedGraphComponent.save(idVersionedNamedGraph, index, idNamedGraph);
    }

    /**
     * Saves and return the RDF node inside the database if it doesn't exist, else returns the existing one.
     *
     * @param spo  The RDF node
     * @param type The RDF node type (logging purpose)
     * @return The saved or existing RDFResourceOrLiteral element
     */
    private RDFResourceOrLiteral saveRDFResourceOrLiteralOrReturnExisting(RDFNode spo, String type) {
        if (spo.isLiteral()) {
            Literal literal = spo.asLiteral();
            String literalValue = literal.getString();

            log.debug("Upsert returning {} literal: {}", type, literalValue);
            return rdfResourceRepository.save(literalValue, spo.asLiteral().getDatatype().getURI());
        } else {

            log.debug("Upsert {} resource: {}", type, spo);
            return rdfResourceRepository.save(spo.toString(), null);
        }
    }

    /**
     * Generates the versioned named graph URI
     *
     * @param namedGraphURI The named graph URI
     * @param versionIndex  The current version index
     * @return The versioned graph URI
     */
    private String generateVersionedNamedGraph(String namedGraphURI, Integer versionIndex) {
        return namedGraphURI + "_" + versionIndex;
    }
}
