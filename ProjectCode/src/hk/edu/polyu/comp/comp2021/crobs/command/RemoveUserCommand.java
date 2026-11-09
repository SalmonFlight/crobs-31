package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;

import java.util.Objects;

/** Removes a regular user and their bookings (REQ3). */
public final class RemoveUserCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 2;
    private final UserService userService;

    /**
     * Constructs the command.
     * @param userService service managing users and cancellation callbacks
     */
    public RemoveUserCommand(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "removeUser";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        session.requireAdmin();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "removeUser targetUserID");
        User user = remember(userService.removeUserWithUndo(args[1]));
        return CommandResult.success("Removed user " + user.getId() + " and their bookings.", user);
    }
}
