package org.hypermedea.ld;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Term;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import java.util.Objects;

public class RDFTripleWrapper {

    public static final String RDF_FUNCTOR = "rdf";

    public static final String RDF_TYPE_MAP_FUNCTOR = "rdf_type_map";

    private static final Atom RDF_TYPE_URI_ATOM = ASSyntax.createAtom("uri");

    private static final Atom RDF_TYPE_BNODE_ATOM = ASSyntax.createAtom("bnode");

    private static final Atom RDF_TYPE_LITERAL_ATOM = ASSyntax.createAtom("literal");

    private Statement triple;

    private Object[] arguments;

    private Term[] annotations;

    public RDFTripleWrapper(Statement triple) {
        this.triple = triple;

        RDFNode s = triple.getSubject();
        RDFNode p = triple.getPredicate();
        RDFNode o = triple.getObject();

        Term subject = getRDFNodeTerm(s);
        Term predicate = getRDFNodeTerm(p);
        Term object = getRDFNodeTerm(o);

        this.arguments = new Object[] { subject, predicate, object };

        Atom subjectType = getRDFTypeAtom(s);
        Atom predicateType = getRDFTypeAtom(p);
        Atom objectType = getRDFTypeAtom(o);
        Term typeMap = ASSyntax.createStructure(RDF_TYPE_MAP_FUNCTOR, subjectType, predicateType, objectType);

        this.annotations = new Term[] { typeMap };
    }

    public String getPropertyName() {
        return RDF_FUNCTOR;
    }

    public Object[] getPropertyArguments() {
        return arguments;
    }

    public Term[] getPropertyAnnotations() {
        return annotations;
    }

    private Term getRDFNodeTerm(RDFNode n) {
        if (n.isURIResource()) return ASSyntax.createString(n.asResource().getURI());
        else if (n.isAnon()) return ASSyntax.createAtom("bnode_" + n.asResource().getId());
        else if (n.asLiteral().getValue() instanceof Number) return ASSyntax.createNumber(n.asLiteral().getDouble());
        else return ASSyntax.createString(n.asLiteral().getString());
    }

    private Atom getRDFTypeAtom(RDFNode n) {
        if (n.isURIResource()) return RDF_TYPE_URI_ATOM;
        else if (n.isAnon()) return RDF_TYPE_BNODE_ATOM;
        else return RDF_TYPE_LITERAL_ATOM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RDFTripleWrapper that = (RDFTripleWrapper) o;
        return Objects.equals(triple, that.triple);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triple);
    }
}
