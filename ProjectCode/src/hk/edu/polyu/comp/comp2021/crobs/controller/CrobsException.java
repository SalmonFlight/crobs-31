package hk.edu.polyu.comp.comp2021.crobs.controller;

/** An expected validation, authorization, or command error. */
public class CrobsException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates an error without a message. */
    public CrobsException() {
        super();
    }

    /**
     * Creates an error with a user-facing message.
     * @param message explanation of the failure
     */
    public CrobsException(String message) {
        super(message);
    }
}
