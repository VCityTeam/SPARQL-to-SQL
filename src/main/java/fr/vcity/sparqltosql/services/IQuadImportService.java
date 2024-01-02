package fr.vcity.sparqltosql.services;

import org.apache.jena.riot.RiotException;
import org.springframework.web.multipart.MultipartFile;

public interface IQuadImportService {

    Integer importModel(MultipartFile file) throws RiotException;

    void resetDatabase();

    void importWorkspace(MultipartFile file) throws RiotException;

    void removeWorkspace();
}
