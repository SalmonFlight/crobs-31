package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
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
     * @throws CrobsException if an error occurs
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        return CommandResult.success("Terminating CROBS...", null);
    }

    /**
     * Undoes the command.
     *
     * @throws CrobsException if an error occurs
     */
    @Override
    public void undo() throws CrobsException {
        // Nothing to undo for quit
    }

    /**
     * Redoes the command.
     *
     * @throws CrobsException if an error occurs
     */
    @Override
    public void redo() throws CrobsException {
        // Nothing to redo for quit
    }
}