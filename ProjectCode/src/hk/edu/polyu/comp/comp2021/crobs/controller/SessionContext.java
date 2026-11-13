package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.Role;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Holds the current login and checks command permissions. */
public class SessionContext {
    private final UserRepository users;
    private final boolean resolveCurrentUser;
    private final List<User> currentUsers = new ArrayList<>(1);

    /** Creates an initially logged-out session. */
    public SessionContext() {
        this.users = new UserRepository();
        this.resolveCurrentUser = false;
    }

    /**
     * Creates a session that observes user deletion and role changes.
     * @param users the application's user repository
     */
    public SessionContext(UserRepository users) {
        this.users = Objects.requireNonNull(users, "User repository is required.");
        this.resolveCurrentUser = true;
    }

    /**
     * Sets the current login.
     * @param user existing user to log in
     */
    public void login(User user) {
        currentUsers.clear();
        currentUsers.add(Objects.requireNonNull(user, "User is required."));
    }

    /** Clears the current login. */
    public void logout() {
        currentUsers.clear();
    }

    /**
     * Gets the current user, resolving its latest role when a repository is supplied.
     * @return the current user, or null when logged out or removed
     */
    public User getCurrentUser() {
        if (resolveCurrentUser && !currentUsers.isEmpty()) {
            User latestUser = users.get(currentUsers.get(0).getId());
            currentUsers.clear();
            if (latestUser != null) {
                currentUsers.add(latestUser);
            }
        }
        return currentUsers.isEmpty() ? null : currentUsers.get(0);
    }

    /**
     * Requires an active login.
     * @return the current user
     * @throws CrobsException when logged out
     */
    public User requireLoggedIn() throws CrobsException {
        User user = getCurrentUser();
        if (user == null) {
            throw new CrobsException("Please log in first.");
        }
        return user;
    }

    /**
     * Requires an admin or superadmin.
     * @return the current administrator
     * @throws CrobsException when permission is missing
     */
    public User requireAdmin() throws CrobsException {
        User user = requireLoggedIn();
        if (user.getRole() == Role.REGULAR) {
            throw new CrobsException("Administrator access is required.");
        }
        return user;
    }

    /**
     * Requires the superadmin.
     * @return the current superadmin
     * @throws CrobsException when permission is missing
     */
    public User requireSuperAdmin() throws CrobsException {
        User user = requireLoggedIn();
        if (user.getRole() != Role.SUPERADMIN) {
            throw new CrobsException("Superadmin access is required.");
        }
        return user;
    }
}
