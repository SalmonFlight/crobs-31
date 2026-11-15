package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/**
 * Command to terminate the CROBS application.
 */
public class QuitCommand implements Command {

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "quit"; }

    /**
     * Executes the quit command.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the command result
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) {
        return CommandResult.success("Terminating CROBS...", null);
    }

    /**
     * Undoes the command.
     */
    @Override
    public void undo() {
        // Nothing to undo for quit
    }

    /**
     * Redoes the command.
     */
    @Override
    public void redo() {
        // Nothing to redo for quit
    }
}