package fr.vcity.sparqltosql.model;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import lombok.Data;

@Data
public class RDFSavedTriple {
    private RDFResourceOrLiteral savedRDFSubject;
    private RDFResourceOrLiteral savedRDFPredicate;
    private RDFResourceOrLiteral savedRDFObject;

    public RDFSavedTriple(RDFResourceOrLiteral savedRDFSubject, RDFResourceOrLiteral savedRDFPredicate, RDFResourceOrLiteral savedRDFObject) {
        this.savedRDFSubject = savedRDFSubject;
        this.savedRDFPredicate = savedRDFPredicate;
        this.savedRDFObject = savedRDFObject;
    }
}
