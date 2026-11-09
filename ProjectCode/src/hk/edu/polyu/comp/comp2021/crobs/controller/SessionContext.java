package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.Role;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRepository;

import java.util.Objects;
import java.util.Optional;

/** Holds the current login and checks command permissions. */
public class SessionContext {
    private final Optional<UserRepository> users;
    private Optional<User> currentUser = Optional.empty();

    /** Creates an initially logged-out session. */
    public SessionContext() {
        this.users = Optional.empty();
    }

    /**
     * Creates a session that observes user deletion and role changes.
     * @param users the application's user repository
     */
    public SessionContext(UserRepository users) {
        this.users = Optional.of(Objects.requireNonNull(users, "User repository is required."));
    }

    /**
     * Sets the current login.
     * @param user existing user to log in
     */
    public void login(User user) {
        currentUser = Optional.of(Objects.requireNonNull(user, "User is required."));
    }

    /** Clears the current login. */
    public void logout() {
        currentUser = Optional.empty();
    }

    /**
     * Gets the current user, resolving its latest role when a repository is supplied.
     * @return the current user, or null when logged out or removed
     */
    public User getCurrentUser() {
        if (users.isPresent()) {
            currentUser = currentUser.flatMap(user -> Optional.ofNullable(users.get().get(user.getId())));
        }
        return currentUser.orElse(null);
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
