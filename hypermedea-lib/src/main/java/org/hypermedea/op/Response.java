package org.hypermedea.op;

import jason.asSyntax.Structure;

import java.util.Collection;

public interface Response {

  enum ResponseStatus {
    OK,
    CLIENT_ERROR,
    SERVER_ERROR,
    UNKNOWN_ERROR
  }

  Operation getOperation();

  ResponseStatus getStatus();

  /**
   * Note: not only for GET/WATCH. If e.g. POST with creation of resource:
   * this method should return the exposed link to new resource as a Jason term.
   *
   * @return a collection of Jason terms representing the response's payload.
   */
  Collection<Structure> getPayload();

}
