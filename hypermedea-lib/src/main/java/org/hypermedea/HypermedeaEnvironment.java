package org.hypermedea;

import jason.asSyntax.Structure;
import jason.environment.Environment;

public class HypermedeaEnvironment extends Environment {

    @Override
    public boolean executeAction(String agName, Structure act) {
        // TODO GET, PUT, POST, DELETE
        return super.executeAction(agName, act);
    }

}
