package org.hypermedea.ct.rdf;

import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;
import org.hypermedea.tools.Identifiers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 *   Handler for representations with any of the RDF Content-Types:
 * </p>
 * <ul>
 *   <li><code>text/turtle</code></li>
 *   <li><code>application/n-triples</code></li>
 *   <li><code>application/ld+json</code></li>
 *   <li><code>application/trig</code></li>
 *   <li><code>application/n-quads</code></li>
 *   <li><code>application/rdf+xml</code></li>
 * </ul>
 *
 * @author Victor Charpenay
 */
public class RDFHandler extends BaseRepresentationHandler {

    public static final String RDF_FUNCTOR = "rdf";

    public static final String RDF_TYPE_MAP_FUNCTOR = "rdf_type_map";

    public static final Atom RDF_TYPE_URI_ATOM = ASSyntax.createAtom("uri");

    public static final Atom RDF_TYPE_BNODE_ATOM = ASSyntax.createAtom("bnode");

    public static final Atom RDF_TYPE_LITERAL_ATOM = ASSyntax.createAtom("literal");

    public static final String[] RDF_CT = {
        "text/turtle", "application/n-triples", "application/ld+json",
        "application/trig", "application/n-quads", "application/rdf+xml"
    };

    public RDFHandler() {
        super(RDF_FUNCTOR, RDF_CT);
    }

    @Override
    public int getArity() {
        return 3;
    }

    public Statement getTriple(Literal t) throws IllegalArgumentException {
        if (t.getArity() != 3)
            throw new IllegalArgumentException("RDF term must be ternary: " + t);

        Term s = t.getTerm(0);
        Term p = t.getTerm(1);
        Term o = t.getTerm(2);

        // TODO or no type map?
        Structure typeMap = (Structure) t.getAnnot(RDF_TYPE_MAP_FUNCTOR);

        Term sType = typeMap.getTerm(0);
        Term pType = typeMap.getTerm(1);
        Term oType = typeMap.getTerm(2);

        RDFNode subject = getTermRDFNode(s, sType);
        RDFNode predicate = getTermRDFNode(p, pType);
        RDFNode object = getTermRDFNode(o, oType);

        return getTripleFromNodes(subject, predicate, object);
    }

    public Literal getLiteral(Statement triple) {
        RDFNode s = triple.getSubject();
        RDFNode p = triple.getPredicate();
        RDFNode o = triple.getObject();

        Term subject = getRDFNodeTerm(s);
        Term predicate = getRDFNodeTerm(p);
        Term object = getRDFNodeTerm(o);

        Atom subjectType = getRDFTypeAtom(s);
        Atom predicateType = getRDFTypeAtom(p);
        Atom objectType = getRDFTypeAtom(o);
        Term typeMap = ASSyntax.createStructure(RDF_TYPE_MAP_FUNCTOR, subjectType, predicateType, objectType);

        Literal fact = ASSyntax.createLiteral(RDF_FUNCTOR, subject, predicate, object);
        fact.addAnnot(typeMap);

        return fact;
    }

    @Override
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException {
        Model m = ModelFactory.createDefaultModel();

        for (Literal t : terms) {
            try {
                m.add(getTriple(t));
            } catch (IllegalArgumentException e) {
                // TODO log
                e.printStackTrace();
            }
        }

        m.write(out, supportedContentTypes.get(0), resourceURI);
    }

    @Override
    public Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException {
        Model m = ModelFactory.createDefaultModel().read(representation, resourceURI, contentType);
        Collection<Literal> facts = new HashSet<>();

        for (Statement triple : m.listStatements().toList()) {
            facts.add(getLiteral(triple));
        }

        return facts;
    }

    private Statement getTripleFromNodes(RDFNode s, RDFNode p, RDFNode o) {
        if (!s.isResource())
            throw new IllegalArgumentException("Non-resource node appears as subject of a triple: " + s);

        if (!p.isURIResource())
            throw new IllegalArgumentException("Non-property node appears as predicate of a triple: " + p);

        Property pp = ResourceFactory.createProperty(((Resource) p).getURI());

        return ResourceFactory.createStatement(s.asResource(), pp, o);
    }

    private RDFNode getTermRDFNode(Term term, Term type) {
        if (type.equals(RDF_TYPE_URI_ATOM)) {
            if (!term.isString() && !term.isAtom())
                throw new IllegalArgumentException("URI term isn't represented as a string: " + term);

            return ResourceFactory.createResource(Identifiers.getLexicalForm(term));
        } else if (type.equals(RDF_TYPE_BNODE_ATOM)) {
            if (!term.isAtom())
                throw new IllegalArgumentException("Bnode term isn't an atom: " + term);

            return new ResourceImpl(AnonId.create(Identifiers.getLexicalForm(term)));
        } else if (type.equals(RDF_TYPE_LITERAL_ATOM)) {
            return getTermRDFLiteral(term);
        } else {
            throw new IllegalArgumentException("Term is of unknown RDF type (" + type + "): " + term);
        }
    }

    private RDFNode getTermRDFLiteral(Term t) {
        String lex = Identifiers.getLexicalForm(t);

        if (t.isNumeric()) {
            try {
                double nb = ((NumberTerm) t).solve();
                int i = (int) nb;

                if (i == nb) return ResourceFactory.createTypedLiteral(lex, XSDDatatype.XSDinteger);
                else return ResourceFactory.createTypedLiteral(lex, XSDDatatype.XSDdouble);
            } catch (NoValueException e) {
                throw new RuntimeException(e);
            }
        }

        // TODO if structure, check if known type (e.g. WKT literal)

        return ResourceFactory.createPlainLiteral(lex);
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

}
