package org.hypermedea.jason;

import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.bb.DefaultBeliefBase;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.hypermedea.ct.rdf.RDFHandler;

import java.util.Iterator;

public class RDFBeliefBase extends DefaultBeliefBase {

    private final Model rdfGraph = ModelFactory.createDefaultModel();

    private final RDFHandler rdfHandler = new RDFHandler();

    @Override
    protected boolean add(Literal l, boolean addInEnd) {
        return add(l);
    }

    @Override
    public boolean add(int index, Literal l) {
        return add(l);
    }

    @Override
    public boolean add(Literal l) {
        if (!isRDFLiteral(l)) return super.add(l);

        try {
            rdfGraph.add(rdfHandler.getTriple(l));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean remove(Literal l) {
        if (!isRDFLiteral(l)) return super.remove(l);

        try {
            rdfGraph.remove(rdfHandler.getTriple(l));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void clear() {
        super.clear();
        rdfGraph.removeAll();
    }

    @Override
    public Iterator<Literal> getCandidateBeliefs(Literal l, Unifier u) {
        // TODO unless l is a variable
        if (!isRDFLiteral(l)) return super.getCandidateBeliefs(l, u);

        // TODO
        return null;
    }

    @Override
    public Iterator<Literal> iterator() {
        return new Iterator<>() {

            private final StmtIterator triples = rdfGraph.listStatements();

            private final Iterator<Literal> others = RDFBeliefBase.super.iterator();

            @Override
            public boolean hasNext() {
                return triples.hasNext() || others.hasNext();
            }

            @Override
            public Literal next() {
                if (triples.hasNext()) return rdfHandler.getLiteral(triples.next());
                else if (others.hasNext()) return others.next();
                else return null;
            }

        };
    }

    @Override
    public Literal contains(Literal l) {
        if (!isRDFLiteral(l)) return super.contains(l);

        try {
            // TODO consider annotations before returning l
            if (rdfGraph.contains(rdfHandler.getTriple(l))) return l;
            else return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public int size() {
        return super.size() + (int) rdfGraph.size();
    }

    private boolean isRDFLiteral(Literal l) {
        return l.getFunctor().equals(RDFHandler.RDF_FUNCTOR) && (l.getArity() == 3 || l.getArity() == 4);
    }

}
