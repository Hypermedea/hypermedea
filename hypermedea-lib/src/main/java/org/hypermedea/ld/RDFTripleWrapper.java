package org.hypermedea.ld;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Term;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import java.util.Objects;

/**
 * <p>
 *   Maps an RDF statement (triple) to a Jason term of the form
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
public class RDFTripleWrapper {

    public static final String RDF_FUNCTOR = "rdf";

    public static final String RDF_TYPE_MAP_FUNCTOR = "rdf_type_map";

    public static final Atom RDF_TYPE_URI_ATOM = ASSyntax.createAtom("uri");

    public static final Atom RDF_TYPE_BNODE_ATOM = ASSyntax.createAtom("bnode");

    public static final Atom RDF_TYPE_LITERAL_ATOM = ASSyntax.createAtom("literal");

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
