package onto.namespaceAPI;

import org.semanticweb.owlapi.model.IRI;

import java.util.List;

public class CascadeNamingStrategy implements NamingStrategy {

    private final List<NamingStrategy> strategies;

    public CascadeNamingStrategy(List<NamingStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public String getNameForIRI(IRI iri) {
        for (NamingStrategy strategy : strategies) {
            String name = strategy.getNameForIRI(iri);
            if (name != null) return name;
        }

        return null;
    }
}
