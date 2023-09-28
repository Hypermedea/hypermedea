package org.hypermedea.op;

/**
 * Exception thrown whenever {@link ProtocolBindings#getBinding(String)} is called
 * but no suitable binding has been registered for the URI scheme provided in the form.
 */
public class BindingNotFoundException extends RuntimeException {}
