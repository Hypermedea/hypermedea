package tools;

import org.semanticweb.owlapi.model.IRI;

import java.io.File;

/**
 * TODO class name somewhat misleading?
 *
 * @author Noé SAFFAF, Victor Charpenay
 */
public class IRITools {

    /**
     * Normalize an identifier to be a valid Jason atom. Performed operations:
     * <ul>
     *     <li>lowerize first character</li>
     *     <li>replace white space with underscore (_)</li>
     * </ul>
     *
     * @param str an identifier represented as a string
     * @return a valid Jason atom identifier
     */
    public static String getJasonAtomIdentifier(String str) {
        if (str == null || str.length() == 0) return null;

        String withLowerCase = str.substring(0, 1).toLowerCase() + str.substring(1);

        String withoutWhiteSpace = withLowerCase.replaceAll("\\s", "_");

        return withoutWhiteSpace;
    }

    public static IRI getFileIRI(String filename) {
        File f = new File(filename);
        return IRI.create(f);
    }

}
