package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;

import java.util.Objects;

/** Logs in as an existing user (REQ5); BON2 excludes login from history. */
public final class LoginCommand implements Command {
    private static final int ARGUMENT_COUNT = 2;
    private final UserService userService;

    /**
     * Constructs the command.
     * @param userService service locating existing users
     */
    public LoginCommand(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "login";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "login userID");
        User user = userService.getUser(args[1]);
        session.login(user);
        return CommandResult.success("Logged in as " + user.getId() + ": " + user.getName(), user);
    }

    /** Login is excluded from BON2 undo history. */
    @Override
    public void undo() {
        // Login is deliberately not reversible.
    }

    /** Login is excluded from BON2 redo history. */
    @Override
    public void redo() {
        // Login is deliberately not reversible.
    }
}
