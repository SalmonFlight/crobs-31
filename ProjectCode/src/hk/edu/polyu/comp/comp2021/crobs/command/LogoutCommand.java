package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

/** Clears the current login (REQ6); BON2 excludes logout from history. */
public final class LogoutCommand implements Command {
    /** Creates a logout command with no service dependencies. */
    public LogoutCommand() {
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "logout";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        CommandArguments.requireCount(args, getName(), 1, "logout");
        session.logout();
        return CommandResult.success("Logged out.", null);
    }

    /** Logout is excluded from BON2 undo history. */
    @Override
    public void undo() {
        // Logout is deliberately not reversible.
    }

    /** Logout is excluded from BON2 redo history. */
    @Override
    public void redo() {
        // Logout is deliberately not reversible.
    }
}
