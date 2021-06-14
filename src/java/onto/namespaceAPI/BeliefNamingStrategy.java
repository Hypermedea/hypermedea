package onto.namespaceAPI;

import onto.classes.OWLAxiomJasonWrapper;
import onto.ontologyAPI.AxiomExtractor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import tools.CSVTools;
import tools.IRITools;

import java.io.*;
import java.util.*;

/**
 * A class to have multiple naming strategies for the AxiomJason objects to add a name based on the full name
 *
 * There are 4 strategies in total:
 * 1) Looking if the concerned class has a rdf:label for which the name of the predicate of the label
 * 2) Looking if the concerned class has a preferredNamespaceUri/preferredNamespacePrefix in which case
 * the name is the adjacent string of the namespace
 * 3) Looking for known prefix in a data base
 * 4) default method where the uri (full name) is sliced through one of the following character "/",":","#"
 * starting from the right and uses the suffix as a name
 *
 * @author Noé Saffaf, Victor Charpenay
 */
public class BeliefNamingStrategy {
    public Map<String,String> mappedLabels;
    public Map<String,String> mappedPreferredNamespaces;
    public Map<String,String> mappedKnownNamespaces;

    public Map<String, String> getMappedKnownNamespaces() {
        return mappedKnownNamespaces;
    }
    public void setMappedKnownNamespaces(HashMap<String, String> mappedKnownNamespaces) { this.mappedKnownNamespaces = mappedKnownNamespaces; }
    public Map<String, String> getMappedLabels() {
        return mappedLabels;
    }
    public void setMappedLabels(HashMap<String, String> mappedLabels) {
        this.mappedLabels = mappedLabels;
    }
    public Map<String, String> getMappedPreferredNamespaces() {
        return mappedPreferredNamespaces;
    }
    public void setMappedPreferredNamespaces(HashMap<String, String> mappedPreferredNamespaces) { this.mappedPreferredNamespaces = mappedPreferredNamespaces; }

    public BeliefNamingStrategy() {
        mappedLabels = new HashMap<>();
        mappedPreferredNamespaces = new HashMap<>();
        mappedKnownNamespaces = new HashMap<>();
    }

    public void computeMappedLabels(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        mappedLabels = AxiomExtractor.extractLabels(owlClassSet, owlOntology);
    }

    public void computeMappedPreferredNamespaces(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        mappedPreferredNamespaces = AxiomExtractor.extractPreferredNamespaces(owlClassSet,owlOntology);
    }

    public void computeMappedKnownNamespaces(){
        mappedKnownNamespaces = CSVTools.readCSV("prefixes.csv");
    }

    public void generateNameBelief(OWLAxiomJasonWrapper axiom, boolean doLabelStrategy, boolean doPreferenceNamespaceStrategy, boolean doKnownPrefixStrategy){
        boolean hasBeenSet = false;
        if (doLabelStrategy) {
            hasBeenSet = executeLabelStrategy(axiom);
        }

        if (!hasBeenSet & doPreferenceNamespaceStrategy){
            hasBeenSet = executePreferenceNamespaceStrategy(axiom);
            //System.out.println(hasBeenSet + " : " + axiom);
        }

        if (!hasBeenSet & doKnownPrefixStrategy){
            hasBeenSet = executeKnownPrefixStrategy(axiom);
        }

        if (!hasBeenSet) {
            executeDefaultStrategy(axiom);
        }
    }

    private boolean executeLabelStrategy(OWLAxiomJasonWrapper axiom) {
        //System.out.println(axiom.getPredicateFullName());
        if (mappedLabels.containsKey(axiom.getPredicateFullName())){
            //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
            if (axiom.getPredicateFullName().contains(mappedLabels.get(axiom.getPredicateFullName()))) {
                //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
                axiom.setPredicateName(IRITools.firstCharToLowerCase(mappedLabels.get(axiom.getPredicateFullName())));
                //System.out.println("used");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean executePreferenceNamespaceStrategy(OWLAxiomJasonWrapper axiom) {
        if (mappedPreferredNamespaces.containsKey(axiom.getPredicateFullName())){
            //System.out.println(axiom.getPredicateFullName() + " : " + mappedPreferredNamespaces.get(axiom.getPredicateFullName()));
            if (axiom.getPredicateFullName().startsWith(mappedPreferredNamespaces.get(axiom.getPredicateFullName()))) {
                String suffix = IRITools.getNameByMatchingPrefix(axiom.getPredicateFullName(),mappedPreferredNamespaces.get(axiom.getPredicateFullName()));
                axiom.setPredicateName(IRITools.firstCharToLowerCase(suffix));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean executeKnownPrefixStrategy(OWLAxiomJasonWrapper axiom) {
        if (mappedKnownNamespaces.containsKey(axiom.getPredicateFullName())){
            //System.out.println(mappedLabels.containsKey(axiom.getPredicateFullName()));
            if (axiom.getPredicateFullName().startsWith(mappedKnownNamespaces.get(axiom.getPredicateFullName()))) {
                String suffix = IRITools.getNameByMatchingPrefix(axiom.getPredicateFullName(),mappedKnownNamespaces.get(axiom.getPredicateFullName()));
                axiom.setPredicateName(IRITools.firstCharToLowerCase(suffix));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void executeDefaultStrategy(OWLAxiomJasonWrapper axiom) {
        axiom.setPredicateName(IRITools.getSuffixIri(axiom.getPredicateFullName(),true));
    }
}
