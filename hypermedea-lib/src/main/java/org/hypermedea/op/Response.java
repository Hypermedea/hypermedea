package org.hypermedea.op;

import ch.unisg.ics.interactions.wot.td.affordances.Link;

import java.util.Collection;
import java.util.Optional;

public interface Response {

  enum ResponseStatus {
    OK,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNKNOWN_ERROR
  }

  Operation getOperation();

  ResponseStatus getStatus();

  Optional<Object> getPayload();

  Collection<Link> getLinks();

}
