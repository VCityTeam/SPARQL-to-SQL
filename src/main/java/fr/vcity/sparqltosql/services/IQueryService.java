package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import fr.vcity.sparqltosql.dto.Workspace;

import java.util.List;

public interface IQueryService {
    List<RDFCompleteVersionedQuad> queryRequestedValidity(String requestedVersions);

    List<RDFCompleteVersionedQuad> queryRequestedVersion(Integer requestedVersion);

    List<RDFCompleteVersionedQuad> querySPARQL(String queryString);

    Workspace getGraphVersion();
}
