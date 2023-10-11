package org.hypermedea.ct.json;

import jason.asSyntax.Term;

import java.util.List;
import java.util.Map;

public interface JsonTermVisitor {

    void visit(Boolean val);

    void visit(Double val);

    void visit(Long val);

    void visit(); // null

    void visit(String val);

    void visit(List<Term> val);

    void visit(Map<String, Term> val);

}
