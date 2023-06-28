package org.hypermedea.sparql;

import jason.NoValueException;
import jason.asSyntax.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.hypermedea.OntologyArtifact;
import org.hypermedea.common.WrapperException;
import org.hypermedea.ld.RDFTripleWrapper;

public class TermTriplePatternWrapper {

    public static final Atom RDF_TYPE_ANY_ATOM = ASSyntax.createAtom("any");

    private final Triple triplePattern;

    public TermTriplePatternWrapper(Pred p) {
        triplePattern = parsePredicate(p);
    }

    public Triple getTriplePattern() {
        return triplePattern;
    }

    private Triple parsePredicate(Pred p) {
        if (p.getFunctor().equals(RDFTripleWrapper.RDF_FUNCTOR)) {
            checkRDFPredicate(p);

            Literal typeMap = p.getAnnot(RDFTripleWrapper.RDF_TYPE_MAP_FUNCTOR);
            Term objectType = (typeMap != null) ? typeMap.getTerm(2) : RDF_TYPE_ANY_ATOM;

            Node subject = getSubjectNode(p.getTerm(0));
            Node predicate = getPredicateNode(p.getTerm(1));
            Node object = getObjectNode(p.getTerm(2), objectType);

            return new Triple(subject, predicate, object);
        } else {
            checkArbitraryPredicate(p);

            Literal predicateURIAnnot = p.getAnnot(OntologyArtifact.PREDICATE_IRI_FUNCTOR);

            if (predicateURIAnnot != null) {
                // TODO get uri and construct triple
            }
        }

        throw new WrapperException(String.format("Predicate %s has no known RDF equivalent", p));
    }

    private void checkRDFPredicate(Pred p) {
        if (p.getArity() != 3) {
            throw new WrapperException(String.format("Predicate %s isn't a triple", p.toStringAsTerm()));
        }

        Literal typeMap = p.getAnnot(RDFTripleWrapper.RDF_TYPE_MAP_FUNCTOR);

        if (typeMap == null && p.getTerm(2).isString()) {
            throw new WrapperException(String.format("Predicate %s has ambiguous typing and has no type map", p.toStringAsTerm()));
        } else if (typeMap != null && typeMap.getArity() != 3) {
            throw new WrapperException(String.format("Predicate %s's type map isn't well-formed", p.toStringAsTerm()));
        }
    }

    private void checkArbitraryPredicate(Pred p) {
        if (p.getArity() > 2) {
            throw new WrapperException(String.format("Predicate %s isn't a triple", p.toStringAsTerm()));
        }

        Literal predicateURI = p.getAnnot(OntologyArtifact.PREDICATE_IRI_FUNCTOR);

        if (predicateURI == null) {
            throw new WrapperException(String.format("Predicate %s has no predicate URI", p.toStringAsTerm()));
        }

        // TODO distinguish object properties and datatype properties

        if (p.getArity() == 2 && p.getTerm(1).isString()) {
            throw new WrapperException(String.format("Predicate %s is ambiguous", p.toStringAsTerm()));
        }
    }

    private Node getPredicateNode(Term node) {
        if (node.isString() || node.isVar()) return getSubjectNode(node);

        throw new WrapperException(String.format("Term %s isn't recognized as URI node or variable", node));
    }

    private Node getSubjectNode(Term node) {
        if (node.isString()) {
            String uri = ((StringTerm) node).getString();
            return NodeFactory.createURI(uri);
        } else if (node.isAtom()) {
            String id = ((Atom) node).getFunctor();
            return NodeFactory.createBlankNode(id);
        } else if (node.isVar()) {
            String name = ((VarTerm) node).getFunctor();
            return NodeFactory.createVariable(name);
        }

        throw new WrapperException(String.format("Term %s isn't recognized as URI node, blank node or variable", node));
    }

    private Node getObjectNode(Term node, Term type) {
        if (node.isAtom()) return getSubjectNode(node);
        if (node.isVar()) return getSubjectNode(node);

        if (node.isNumeric()) {
            try {
                Double nb = ((NumberTerm) node).solve();
                return NodeFactory.createLiteral(nb.toString(), XSDDatatype.XSDdouble);
            } catch (NoValueException e) {
                throw new WrapperException(e);
            }
        } else if (node.isString()) {
            if (type.equals(RDFTripleWrapper.RDF_TYPE_URI_ATOM)) {
                String uri = ((StringTerm) node).getString();
                return NodeFactory.createURI(uri);
            } else if (type.equals(RDFTripleWrapper.RDF_TYPE_LITERAL_ATOM)) {
                String lexicalValue = ((StringTerm) node).getString();
                return NodeFactory.createLiteral(lexicalValue);
            }
        }

        throw new WrapperException(String.format("Term %s with type %s isn't recognized as an RDF node", node, type));
    }

}
