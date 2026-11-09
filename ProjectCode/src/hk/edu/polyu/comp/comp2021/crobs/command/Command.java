package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/**
 * Interface representing a command in the CROBS system.
 */
public interface Command {

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    String getName();

    /**
     * Executes the command with the given arguments.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the result of the command execution
     * @throws CrobsException if an error occurs during execution
     */
    CommandResult execute(String[] args, SessionContext session) throws CrobsException;

    /**
     * Undoes the command.
     *
     * @throws CrobsException if undo fails
     */
    void undo() throws CrobsException;

    /**
     * Redoes the command.
     *
     * @throws CrobsException if redo fails
     */
    void redo() throws CrobsException;
}