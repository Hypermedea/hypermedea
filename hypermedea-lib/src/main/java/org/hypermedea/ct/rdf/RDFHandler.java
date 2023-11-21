package org.hypermedea.ct.rdf;

import jason.asSyntax.*;
import jason.asSyntax.Literal;
import org.apache.jena.rdf.model.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;

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
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException {
        Model m = ModelFactory.createDefaultModel();

        for (Literal t : terms) {
            try {
                if (t.getArity() != 3) continue;

                Term s = t.getTerm(0);
                Term p = t.getTerm(1);
                Term o = t.getTerm(2);

                Term typeMap = t.getAnnot(RDF_TYPE_MAP_FUNCTOR);
                if (!typeMap.isStructure()) continue;

                Structure typeMap2 = (Structure) typeMap;
                Term sType = typeMap2.getTerm(0);
                Term pType = typeMap2.getTerm(1);
                Term oType = typeMap2.getTerm(2);

                RDFNode subject = getNode(s, sType);
                RDFNode predicate = getNode(p, pType);
                RDFNode object = getNode(o, oType);

                m.add(getStatement(subject, predicate, object));
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

            facts.add(fact);
        }

        return facts;
    }

    private Statement getStatement(RDFNode s, RDFNode p, RDFNode o) {
        if (!s.isResource())
            throw new IllegalArgumentException("Non-resource node appears as subject of a triple: " + s);

        if (!p.isURIResource())
            throw new IllegalArgumentException("Non-property node appears as predicate of a triple: " + p);

        Property pp = ResourceFactory.createProperty(((Resource) p).getURI());

        return ResourceFactory.createStatement(s.asResource(), pp, o);
    }

    private RDFNode getNode(Term term, Term type) {
        if (type.equals(RDF_TYPE_URI_ATOM)) {
            if (!term.isString() && !term.isAtom())
                throw new IllegalArgumentException("URI term isn't represented as a string: " + term);

            return ResourceFactory.createResource(((StringTerm) term).getString());
        } else if (type.equals(RDF_TYPE_BNODE_ATOM)) {
            if (!term.isAtom())
                throw new IllegalArgumentException("Bnode term: " + term);

            return ResourceFactory.createResource(); // FIXME should have a nodeID
        } else if (type.equals(RDF_TYPE_LITERAL_ATOM)) {
            // TODO proper datatype
            return ResourceFactory.createPlainLiteral(term.toString());
        } else {
            throw new IllegalArgumentException("Term is of unknown RDF type (" + type + "): " + term);
        }
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
