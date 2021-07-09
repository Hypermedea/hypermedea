package org.hypermedea.tools;

import jason.asSyntax.*;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;

/**
 * Various methods to handle identifiers (Jason/PDDL identifiers, IRIs).
 *
 * @author Noé SAFFAF, Victor Charpenay
 */
public class Identifiers {

    /**
     * Normalize an identifier to be a valid Jason atom. Performed operations:
     * <ul>
     *     <li>lowerize first character</li>
     *     <li>replace white space with underscore (_)</li>
     *     <li>replace all non-word characters (not in [a-zA-Z_0-9]) with underscore (_)</li>
     * </ul>
     *
     * @param str an identifier represented as a string
     * @return a valid Jason atom identifier
     */
    public static String getJasonAtomIdentifier(String str) {
        if (str == null || str.length() == 0) return null;

        String withLowerCase = str.substring(0, 1).toLowerCase() + str.substring(1);

        String withoutWhiteSpace = withLowerCase.replaceAll("\\s", "_");

        String withAsciiOnly = withoutWhiteSpace.replaceAll("\\W", "_");

        return withAsciiOnly;
    }

    /**
     * Return the lexical form (a string) of a Jason term. E.g. remove quotes from Jason strings.
     *
     * @param t a Jason term
     * @return <code>t</code>'s lexical form
     */
    public static String getLexicalForm(Term t) {
        if (t.isAtom()) return t.toString();
        else if (t.isString()) return ((StringTerm) t).getString();

        return null;
    }

    public static IRI getFileIRI(String filename) {
        File f = new File(filename);
        return IRI.create(f);
    }

}
