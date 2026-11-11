package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Role;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;

import java.util.Objects;

/** Lets the superadmin change another user's regular/admin role (REQ4). */
public final class ChangeRoleCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 3;
    private static final int ROLE_ARGUMENT_INDEX = 2;
    private final UserService userService;

    /**
     * Constructs the command.
     * @param userService service managing user roles
     */
    public ChangeRoleCommand(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "changeRole";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        User actor = session.requireSuperAdmin();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "changeRole targetUserId regular|admin");
        if (actor.getId().equals(args[1])) {
            throw new CrobsException("The superadmin can only change other users' roles.");
        }
        Role role;
        if ("regular".equalsIgnoreCase(args[ROLE_ARGUMENT_INDEX])
                || "regularUser".equalsIgnoreCase(args[ROLE_ARGUMENT_INDEX])) {
            role = Role.REGULAR;
        } else if ("admin".equalsIgnoreCase(args[ROLE_ARGUMENT_INDEX])) {
            role = Role.ADMIN;
        } else {
            throw new CrobsException("Role must be regular or admin.");
        }
        User user = remember(userService.changeRoleWithUndo(args[1], role));
        return CommandResult.success("Changed user " + user.getId() + " to " + role + ".", user);
    }
}
