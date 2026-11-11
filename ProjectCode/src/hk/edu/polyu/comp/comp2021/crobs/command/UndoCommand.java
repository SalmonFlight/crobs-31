package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/**
 * Command to undo the last action.
 */
public class UndoCommand implements Command {
    @Override
    public String getName() { return "undo"; }

    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        return CommandResult.success("Undo processed.", null);
    }

    @Override
    public void undo() throws CrobsException {}

    @Override
    public void redo() throws CrobsException {}
}