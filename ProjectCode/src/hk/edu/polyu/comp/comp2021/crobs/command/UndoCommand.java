package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/**
 * Command to undo the last action.
 */
public class UndoCommand implements Command {

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "undo"; }

    /**
     * Executes the undo command.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the command result
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) {
        return CommandResult.success("Undo processed.", null);
    }

    /**
     * Undoes the command.
     */
    @Override
    public void undo() {}

    /**
     * Redoes the command.
     */
    @Override
    public void redo() {}
}