package org.hypermedea.onto;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite short form provider, which takes an ordered list of delegate providers. When asked to provide a short form,
 * the cascade provider returns the value of the first delegate that doesn't yield an empty string.
 *
 * FIXME most already implemented short form provider already include a default backup...
 *
 * @author Victor Charpenay
 */
public class CascadeShortFormProvider implements ShortFormProvider {

    private final List<ShortFormProvider> delegates = new ArrayList<>();

    public CascadeShortFormProvider(ShortFormProvider... delegates) {
        for (ShortFormProvider d : delegates) this.delegates.add(d);
    }

    @Override
    public String getShortForm(OWLEntity owlEntity) {
        for (ShortFormProvider d : delegates) {
            String shortForm = d.getShortForm(owlEntity);
            if (!shortForm.isEmpty()) return shortForm;
        }

        return "";
    }

    @Override
    public void dispose() {
        for (ShortFormProvider d : delegates) d.dispose();
    }

}
