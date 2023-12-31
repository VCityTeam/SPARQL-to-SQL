package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.RDFNamedGraph;
import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import fr.vcity.sparqltosql.exceptions.FileException;
import fr.vcity.sparqltosql.model.RDFSavedQuad;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IRDFNamedGraphRepository rdfNamedGraphRepository;
    IRDFCommitRepository rdfCommitRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QuadImportService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IRDFNamedGraphRepository rdfNamedGraphRepository,
            IRDFCommitRepository rdfCommitRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
        this.rdfCommitRepository = rdfCommitRepository;
    }

    /**
     * Import RDF statements represented in language <code>lang</code> to the model as valid in the new version.
     * <br />Predefined values for <code>lang</code> are "TRIG" and "NQUADS"
     *
     * @param files The input files
     */
    @Override
    public void importModelToAdd(List<MultipartFile> files) {
        Integer maxLength = rdfVersionedQuadRepository.getMaxValidity();
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
        rdfCommitRepository.save(summarizeImport(fileList, "add"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();

                importDefaultModel(dataset.getDefaultModel(), "add", maxLength);

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());

                    for (StmtIterator s = model.listStatements(); s.hasNext(); ) {
                        RDFSavedQuad rdfSavedQuad = getRDFSavedQuad(s.nextStatement(), namedModel.getURI());

                        rdfVersionedQuadComponent.saveAdd(
                                rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                                maxLength == null ? 0 : maxLength + 1
                        );
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
    }

    /**
     * Import RDF statements represented in language <code>lang</code> to the model as not valid in the new version.
     * <br />Predefined values for <code>lang</code> are "TRIG" and "NQUADS"
     *
     * @param files The input files
     */
    @Override
    public void importModelToRemove(List<MultipartFile> files) {
        Integer maxLength = rdfVersionedQuadRepository.getMaxValidity();
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
        rdfCommitRepository.save(summarizeImport(fileList, "remove"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();

                importDefaultModel(dataset.getDefaultModel(), "remove", maxLength);

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());

                    for (StmtIterator s = model.listStatements(); s.hasNext(); ) {
                        RDFSavedQuad rdfSavedQuad = getRDFSavedQuad(s.nextStatement(), namedModel.getURI());

                        rdfVersionedQuadComponent.saveRemove(
                                rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                                rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                                maxLength == null ? 0 : maxLength + 1
                        );
                    }
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
    }

    /**
     * Import RDF statements inside the <code>files</code>
     *
     * @param files The input files
     */
    @Override
    public void importModelToRemoveAndAdd(List<MultipartFile> files) {
        Integer maxLength = rdfVersionedQuadRepository.getMaxValidity();
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .sorted(getRemoveAddFileComparator())
                .collect(Collectors.toList());
        rdfCommitRepository.save(summarizeImport(fileList, "remove-add"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();

                if (file.getOriginalFilename().contains("add")) {
                    importDefaultModel(dataset.getDefaultModel(), "add", maxLength);
                } else if (file.getOriginalFilename().contains("remove")) {
                    importDefaultModel(dataset.getDefaultModel(), "remove", maxLength);
                } else {
                    throw new FileException("The file: " + file.getOriginalFilename() + " doesn't contain 'add' or 'remove'");
                }

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());

                    for (StmtIterator s = model.listStatements(); s.hasNext(); ) {
                        RDFSavedQuad rdfSavedQuad = getRDFSavedQuad(s.nextStatement(), namedModel.getURI());

                        if (file.getOriginalFilename().contains("add")) {
                            rdfVersionedQuadComponent.saveAdd(
                                    rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                                    maxLength == null ? 0 : maxLength + 1
                            );
                        } else {
                            rdfVersionedQuadComponent.saveRemove(
                                    rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                                    rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                                    maxLength == null ? 0 : maxLength + 1
                            );
                        }
                    }
                }
            } catch (IOException e) {
                throw new FileException("Failed to store file.", e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
    }

    /**
     * Deletes all the elements inside the database
     */
    @Override
    public void resetDatabase() {
        rdfVersionedQuadRepository.deleteAll();
        rdfResourceRepository.deleteAll();
        rdfNamedGraphRepository.deleteAll();
        rdfCommitRepository.deleteAll();
    }

    /**
     * Summarize the new version
     *
     * @param fileList   The non empty files
     * @param actionType the action type
     * @return The computed summary of the import
     */
    private String summarizeImport(List<MultipartFile> fileList, String actionType) {
        return String.format("%s: [%s]", actionType, fileList
                .stream()
                .map(multipartFile -> "(" + multipartFile.getOriginalFilename() + ")")
                .collect(Collectors.joining(",")));
    }

    /**
     * Import RDF default model statements
     *
     * @param defaultModel The default graph
     * @param action       The action (add or remove)
     * @param maxLength    The size of the bit string
     */
    private void importDefaultModel(Model defaultModel, String action, Integer maxLength) {
        for (StmtIterator s = defaultModel.listStatements(); s.hasNext(); ) {
            RDFSavedQuad rdfSavedQuad = getRDFSavedQuad(s.nextStatement(), "default");

            if (action.equals("remove")) {
                rdfVersionedQuadComponent.saveRemove(
                        rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                        maxLength == null ? 0 : maxLength + 1
                );
            } else {
                rdfVersionedQuadComponent.saveAdd(
                        rdfSavedQuad.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFObject().getIdResourceOrLiteral(),
                        rdfSavedQuad.getSavedRDFNamedGraph().getIdNamedGraph(),
                        maxLength == null ? 0 : maxLength + 1
                );
            }
        }
    }

    /**
     * Saves the subject, the property, the object and the named graph inside the database if they exist else returning them
     *
     * @param statement  The statement
     * @param namedModel The named model
     * @return The saved or existing Quad
     */
    private RDFSavedQuad getRDFSavedQuad(Statement statement, String namedModel) {
        RDFNode subject = statement.getSubject();
        RDFNode predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        RDFNamedGraph savedRDFNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel);
        RDFResourceOrLiteral savedRDFSubject = saveRDFResourceOrLiteralOrReturnExisting(subject, "Subject");
        RDFResourceOrLiteral savedRDFPredicate = saveRDFResourceOrLiteralOrReturnExisting(predicate, "Predicate");
        RDFResourceOrLiteral savedRDFObject = saveRDFResourceOrLiteralOrReturnExisting(object, "Object");

        log.debug("Insert or updated quad (NG: {}, S: {}, P: {}, O: {})",
                namedModel,
                savedRDFSubject.getName(),
                savedRDFPredicate.getName(),
                savedRDFObject.getName()
        );

        return new RDFSavedQuad(savedRDFNamedGraph, savedRDFSubject, savedRDFPredicate, savedRDFObject);
    }

    /**
     * Saves and return the RDF Named Graph inside the database if it doesn't exist, else returns the existing one.
     *
     * @param uri The RDF named graph URI
     * @return The saved or existing RDFNamedGraph element
     */
    private RDFNamedGraph saveRDFNamedGraphOrReturnExisting(String uri) {
        Optional<RDFNamedGraph> optionalRDFNamedGraph = rdfNamedGraphRepository.findByName(uri);

        if (optionalRDFNamedGraph.isPresent()) {
            log.debug("Found named graph: {}", uri);

            return optionalRDFNamedGraph.get();
        }

        log.debug("Insert named graph: {}", uri);
        return rdfNamedGraphRepository.save(uri);
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

            Optional<RDFResourceOrLiteral> optionalRDFResourceOrLiteral =
                    rdfResourceRepository.findByNameAndType(literalValue, literal.getDatatype().toString());

            if (optionalRDFResourceOrLiteral.isPresent()) {
                log.debug("Found {} literal: {}", type, literalValue);

                return optionalRDFResourceOrLiteral.get();
            }

            log.debug("Insert {} resource: {}", type, literalValue);
            return rdfResourceRepository.save(literalValue, spo.asLiteral().getDatatype().toString());
        } else {

            // Get element if exists or save new
            Optional<RDFResourceOrLiteral> optionalRDFResourceOrLiteral =
                    rdfResourceRepository.findByNameAndType(spo.toString(), null);

            if (optionalRDFResourceOrLiteral.isPresent()) {
                log.debug("Found {} resource: {}", type, spo);

                return optionalRDFResourceOrLiteral.get();
            }

            log.debug("Insert {} resource: {}", type, spo);
            return rdfResourceRepository.save(spo.toString(), null);
        }
    }

    /**
     * Compares two multipartfile (sorting "remove" first and "add" then)
     *
     * @return The comparison
     */
    private static Comparator<MultipartFile> getRemoveAddFileComparator() {
        return (o1, o2) -> {
            if (o1.getOriginalFilename() == null || o2.getOriginalFilename() == null) {
                throw new FileException("The filename is broken");
            }

            if (o1.getOriginalFilename().contains("remove") && o2.getOriginalFilename().contains("remove")) {
                return 0;
            } else if (o1.getOriginalFilename().contains("add") && o2.getOriginalFilename().contains("add")) {
                return 0;
            } else if (o1.getOriginalFilename().contains("remove") && o2.getOriginalFilename().contains("add")) {
                return 1;
            }
            return -1;
        };
    }
}
