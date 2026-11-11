package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/**
 * Command to redo the last undone action.
 */
public class RedoCommand implements Command {
    @Override
    public String getName() { return "redo"; }

    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        return CommandResult.success("Redo processed.", null);
    }

    @Override
    public void undo() throws CrobsException {}

    @Override
    public void redo() throws CrobsException {}
}