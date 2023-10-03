package org.hypermedea.ct.rdf;

import jason.asSyntax.*;
import org.apache.jena.rdf.model.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 *   Map RDF statements (triples) to/from Jason terms of the form
 * </p>
 * <pre>rdf(S, P, O)[ rdf_type_map(uri|bnode, uri, uri|bnode|literal) ]</pre>
 *
 * <p>
 *   The mapping from RDF to Jason is as follows:
 * </p>
 * <table>
 *   <tr>
 *       <th>RDF type</th>
 *       <th>Jason type</th>
 *       <th>Value of <code>rdf_type_map</code></th>
 *   </tr>
 *   <tr>
 *       <td>Named resource (URI)</td>
 *       <td>String</td>
 *       <td><code>uri</code></td>
 *   </tr>
 *   <tr>
 *       <td>Blank node</td>
 *       <td>Atom</td>
 *       <td><code>bnode</code></td>
 *   </tr>
 *   <tr>
 *       <td>Literal (number)</td>
 *       <td>Number</td>
 *       <td><code>literal</code></td>
 *   </tr>
 *   <tr>
 *       <td>Literal (any other type)</td>
 *       <td>String</td>
 *       <td><code>literal</code></td>
 *   </tr>
 * </table>
 *
 * <p>
 *     For example, the RDF graph and Jason belief base given below are equivalent:
 * </p>
 * <pre>ex:alice a ex:Person .
 *ex:alice ex:name "Alice" .
 *ex:alice ex:age 42 .
 *ex:alice ex:knows _:someone .</pre>
 * <pre>rdf("http://example.org/alice", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://example.org/Person") [ rdf_type_map(uri, uri, uri) ] .
 *rdf("http://example.org/alice", "http://example.org/name", "Alice") [ rdf_type_map(uri, uri, literal) ] .
 *rdf("http://example.org/alice", "http://example.org/age", 42) [ rdf_type_map(uri, uri, literal) ] .
 *rdf("http://example.org/alice", "http://example.org/knows", someone) [ rdf_type_map(uri, uri, bnode) ] .</pre>
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
    public void serialize(Collection<Structure> terms, OutputStream out) throws UnsupportedRepresentationException {
        Model m = ModelFactory.createDefaultModel();

        for (Structure t : terms) {
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

        m.write(out, supportedContentTypes.get(0));
    }

    @Override
    public Collection<Structure> deserialize(InputStream representation, String contentType) throws UnsupportedRepresentationException {
        Model m = ModelFactory.createDefaultModel().read(representation, contentType);
        Collection<Structure> facts = new HashSet<>();

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

            Structure fact = ASSyntax.createStructure(RDF_FUNCTOR, subject, predicate, object);
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

        return ResourceFactory.createStatement((Resource) s, (Property) p, o);
    }

    private RDFNode getNode(Term term, Term type) {
        if (type.equals(RDF_TYPE_URI_ATOM)) {
            if (!term.isString() || !term.isAtom())
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
