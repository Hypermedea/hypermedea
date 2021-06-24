package onto.namespaceAPI;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.LinkedHashSet;
import java.util.Set;

public class NamingStrategyFactory {

    public enum NamingStrategyType {
        LABEL,PREFERRED_NAMESPACE,KNOWN_NAMESPACE,DEFAULT
    }

    public static NamingStrategy createNamingStrategy(OWLOntology owlOntology) {
        // TODO
        return null;
    }

    /**
     * Default Method to get only the default naming strategy
     * @return default naming strategy
     */
    public static Set<NamingStrategy> createDefaultNamingStrategySet() {
        Set<NamingStrategy> namingStrategySet = new LinkedHashSet<>();
        namingStrategySet.add(new DefaultNamingStrategy());
        return namingStrategySet;
    }

    /**
     * Return a set of naming strategy depending on which types are desired
     * @param namingStrategyTypeSet Set of strategies' type
     * @return set of naming strategies
     */
    public static Set<NamingStrategy> createNamingStrategySet(Set<NamingStrategyType> namingStrategyTypeSet){
        Set<NamingStrategy> namingStrategySet = new LinkedHashSet<>();
        for (NamingStrategyType namingStrategyType: namingStrategyTypeSet){
            switch (namingStrategyType){
                case LABEL:
                    namingStrategySet.add(new LabelNamingStrategy());
                    break;
                case PREFERRED_NAMESPACE:
                    namingStrategySet.add(new PreferredNamespaceNamingStrategy());
                    break;
                case KNOWN_NAMESPACE:
                    namingStrategySet.add(new KnownNamespaceNamingStrategy());
                    break;
                case DEFAULT:
                    namingStrategySet.add(new DefaultNamingStrategy());
            }
        }
        return namingStrategySet;
    }

    /**
     * Return a set of all implemented strategies through a specific order
     * (Label -> PreferredNamespaces -> KnownNamespaces/Prefix -> Default)
     * @return
     */
    public static Set<NamingStrategy> createAllNamingStrategySet() {
        Set<NamingStrategy> namingStrategySet = new LinkedHashSet<>();
        namingStrategySet.add(new LabelNamingStrategy());
        namingStrategySet.add(new PreferredNamespaceNamingStrategy());
        namingStrategySet.add(new KnownNamespaceNamingStrategy());
        namingStrategySet.add(new DefaultNamingStrategy());
        return namingStrategySet;
    }

}
