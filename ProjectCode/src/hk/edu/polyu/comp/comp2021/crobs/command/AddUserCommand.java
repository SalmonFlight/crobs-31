package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;

import java.util.Arrays;
import java.util.Objects;

/** Creates a regular user on behalf of an administrator (REQ2). */
public final class AddUserCommand extends AbstractUndoableCommand {
    private static final int MINIMUM_ARGUMENT_COUNT = 2;
    private final UserService userService;

    /**
     * Constructs the command.
     * @param userService service managing users
     */
    public AddUserCommand(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "addUser";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        session.requireAdmin();
        CommandArguments.requireMinimum(args, getName(), MINIMUM_ARGUMENT_COUNT, "addUser name");
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        User user = remember(userService.addUserWithUndo(name));
        return CommandResult.success("Added user " + user.getId() + ": " + user.getName(), user);
    }
}
