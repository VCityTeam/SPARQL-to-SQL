package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.ScenarioVersion;
import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Scenario;
import fr.vcity.sparqltosql.dto.Space;
import fr.vcity.sparqltosql.dto.Workspace;
import fr.vcity.sparqltosql.repository.*;
import fr.vcity.sparqltosql.utils.SPARQLtoSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.walker.Walker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryService implements IQueryService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IRDFVersionedNamedGraphRepository rdfNamedGraphRepository;
    IVersionedWorkspaceRepository versionedWorkspaceRepository;
    IVersionRepository versionRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QueryService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IRDFVersionedNamedGraphRepository rdfNamedGraphRepository,
            IVersionRepository versionRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent,
            IVersionedWorkspaceRepository versionedWorkspaceRepository
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfNamedGraphRepository = rdfNamedGraphRepository;
        this.versionRepository = versionRepository;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
        this.versionedWorkspaceRepository = versionedWorkspaceRepository;
    }

    /**
     * @param requestedValidity the request validity
     * @return the quads list filtered by requestedValidity
     */
    @Override
    public List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedValidity) {
        log.debug("Requested: {}", requestedValidity);

        if (requestedValidity.equals("*")) {
            return rdfVersionedQuadComponent.findAll();
        }

        return rdfVersionedQuadComponent
                .findAllByValidity(requestedValidity);
    }

    /**
     * @param requestedVersion the request version number
     * @return the quads list filtered by requestedVersion where the fact has to be truthy
     */
    @Override
    public List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion) {
        log.debug("Requested version: {}", requestedVersion);

        return rdfVersionedQuadComponent.findAllByVersion(requestedVersion);
    }

    /**
     * Parse the query String into an algebra
     *
     * @param queryString The given query string
     */
    @Override
    public List<RDFCompleteVersionedQuad> querySPARQL(String queryString) {
        getOperatorsFromQuery(queryString);
        return Collections.emptyList();
    }

    /**
     * Returns the whole graph version of the database
     */
    @Override
    public Workspace getGraphVersion() {
        List<ScenarioVersion> scenarios = versionedWorkspaceRepository.getAllScenarios();

        Workspace workspace = new Workspace();

        Map<String, List<ScenarioVersion>> spaceMap = scenarios
                .stream()
                .collect(Collectors.groupingBy(ScenarioVersion::getSpaceType));

        for (Map.Entry<String, List<ScenarioVersion>> entry : spaceMap.entrySet()) {
            String s = entry.getKey();
            List<ScenarioVersion> scenarioVersions = entry.getValue();
            Map<String, List<ScenarioVersion>> scenariosMap = scenarioVersions
                    .stream()
                    .filter(scenarioVersion -> scenarioVersion.getSpaceType().equals(s))
                    .collect(Collectors.groupingBy(ScenarioVersion::getScenario));

            List<Scenario> computedScenarioList = new ArrayList<>();

            scenariosMap.forEach((sc, scVersions) -> {
                ScenarioVersion currentSV = scenariosMap.get(sc).getFirst();

                List<String> versions = new ArrayList<>();
                while (currentSV != null) {
                    if (currentSV.getPreviousVersion() != null && !versions.contains(currentSV.getPreviousVersion())) {
                        versions.add(currentSV.getPreviousVersion());
                    }
                    versions.add(currentSV.getVersion());
                    ScenarioVersion finalCurrentSV = currentSV;
                    currentSV = scenariosMap
                            .get(sc)
                            .stream()
                            .filter(
                                    scenarioVersion ->
                                            scenarioVersion.getScenario().equals(sc) &&
                                            scenarioVersion.getPreviousVersion() != null &&
                                            scenarioVersion.getPreviousVersion().equals(finalCurrentSV.getVersion())
                            )
                            .findFirst()
                            .orElse(null);
                }

                computedScenarioList.add(new Scenario(sc, versions));
            });

            workspace.setSpace(new Space(
                    s,
                    computedScenarioList
            ));
        }

        return workspace;
    }

    /**
     * Returns the algebra of the given query string
     *
     * @param queryString The query string
     */
    private void getOperatorsFromQuery(String queryString) {
        try {
            Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
            SPARQLtoSQLVisitor sparqLtoSQLVisitor = new SPARQLtoSQLVisitor();
            switch (query.queryType()) {
                case SELECT -> {
                    log.info("******* Op walker - OpVisitor *******");
                    Op op = Algebra.compile(query);
                    Walker.walk(op, sparqLtoSQLVisitor);
                }
                case ASK, CONSTRUCT, DESCRIBE, CONSTRUCT_JSON ->
                        log.warn("Query with type: {} not implemented", query.queryType().toString());
                case UNKNOWN -> log.warn("Unknown query not supported. Ignoring it.");
            }
        } catch (QueryParseException e) {
            log.error(e.getMessage());
            log.warn("Query: {}", queryString);
            log.warn("Info: INSERT, UPDATE queries are not supported by the Query class");
        }
    }
}
