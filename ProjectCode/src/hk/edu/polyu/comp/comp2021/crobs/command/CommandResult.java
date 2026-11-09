package hk.edu.polyu.comp.comp2021.crobs.command;

/**
 * Represents the result of executing a command.
 */
public final class CommandResult {
    private final boolean success;
    private final String message;
    private final Object payload;

    private CommandResult(boolean success, String message, Object payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    /**
     * Creates a successful command result.
     *
     * @param message the success message
     * @param payload optional data returned by the command
     * @return a successful CommandResult
     */
    public static CommandResult success(String message, Object payload) {
        return new CommandResult(true, message, payload);
    }

    /**
     * Creates a failed command result.
     *
     * @param message the error message
     * @return a failed CommandResult
     */
    public static CommandResult failure(String message) {
        return new CommandResult(false, message, null);
    }

    /**
     * Checks if the command was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() { return success; }

    /**
     * Gets the result message.
     *
     * @return the message
     */
    public String getMessage() { return message; }

    /**
     * Gets the payload data.
     *
     * @return the payload object
     */
    public Object getPayload() { return payload; }
}
