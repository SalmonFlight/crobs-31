package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.util.IdGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** User creation, access changes, and reversible cascading deletion. */
public class UserService {
    private final UserRepository users;
    private final BookingCancellation bookings;
    private final User superAdmin;

    /**
     * Creates the unique superadmin when the user domain is initialized.
     * @param users the application's user repository
     * @param bookings Member B's reversible booking cancellation adapter
     */
    public UserService(UserRepository users, BookingCancellation bookings) {
        this.users = Objects.requireNonNull(users, "User repository is required.");
        this.bookings = Objects.requireNonNull(bookings, "Booking cancellation is required.");
        User existing = null;
        for (User user : users.getAll()) {
            if (user.getRole() == Role.SUPERADMIN) {
                if (existing != null) {
                    throw new IllegalArgumentException("There must be exactly one superadmin.");
                }
                existing = user;
            }
        }
        if (existing == null) {
            existing = new SuperAdmin(nextId(), "Super Admin");
            users.add(existing);
        }
        superAdmin = existing;
    }

    /**
     * Gets the automatically initialized superadmin.
     * @return the unique superadmin
     */
    public User getSuperAdmin() {
        return superAdmin;
    }

    /**
     * Finds an existing user.
     * @param userId the user ID
     * @return the user
     * @throws CrobsException when the user does not exist
     */
    public User getUser(String userId) throws CrobsException {
        User user = userId == null ? null : users.get(userId);
        if (user == null) {
            throw new CrobsException("User not found: " + userId);
        }
        return user;
    }

    /**
     * Gets a detached list of users sorted by ID.
     * @return all users
     */
    public List<User> getAllUsers() {
        List<User> result = users.getAll();
        result.sort(Comparator.comparing(User::getId));
        return result;
    }

    /**
     * Adds a regular user with a generated ID.
     * @param name a nonblank display name
     * @return the new user
     * @throws CrobsException when the name is invalid
     */
    public User addUser(String name) throws CrobsException {
        return addUserWithUndo(name).getValue();
    }

    /**
     * Adds a regular user and records the inverse operation.
     * @param name a nonblank display name
     * @return the applied creation
     * @throws CrobsException when the name is invalid
     */
    public ReversibleChange<User> addUserWithUndo(String name) throws CrobsException {
        if (name == null || name.trim().isEmpty()) {
            throw new CrobsException("User name must not be blank.");
        }
        User user = new RegularUser(nextId(), name.trim());
        users.add(user);
        return ReversibleChange.applied(user, () -> {
            requireCurrent(user);
            users.remove(user.getId());
        }, () -> {
            requireVacant(user.getId());
            users.add(user);
        });
    }

    /**
     * Removes a regular user and all their bookings.
     * @param targetUserId the user ID
     * @throws CrobsException when the target is missing or is an administrator
     */
    public void removeUser(String targetUserId) throws CrobsException {
        removeUserWithUndo(targetUserId);
    }

    /**
     * Removes a regular user with a reversible booking cascade.
     * @param targetUserId the user ID
     * @return the applied deletion
     * @throws CrobsException when validation or cancellation fails
     */
    public ReversibleChange<User> removeUserWithUndo(String targetUserId) throws CrobsException {
        User user = getUser(targetUserId);
        if (user.getRole() != Role.REGULAR) {
            throw new CrobsException("Only regular users can be removed.");
        }
        ReversibleChange<Void> cancellation = bookings.cancelBookingsByUser(targetUserId);
        requireCancellation(cancellation);
        users.remove(targetUserId);
        return ReversibleChange.applied(user, () -> {
            requireVacant(user.getId());
            users.add(user);
            try {
                cancellation.undo();
            } catch (CrobsException failure) {
                users.remove(user.getId());
                throw failure;
            }
        }, () -> {
            requireCurrent(user);
            cancellation.redo();
            users.remove(user.getId());
        });
    }

    /**
     * Changes another user's regular/admin role while preserving their identity.
     * @param targetUserId the target ID
     * @param newRole REGULAR or ADMIN
     * @throws CrobsException when the change would affect superadmin uniqueness
     */
    public void changeRole(String targetUserId, Role newRole) throws CrobsException {
        changeRoleWithUndo(targetUserId, newRole);
    }

    /**
     * Records a role change using the corresponding user subclass.
     * @param targetUserId the target ID
     * @param newRole REGULAR or ADMIN
     * @return the applied role change
     * @throws CrobsException when the role or target is invalid
     */
    public ReversibleChange<User> changeRoleWithUndo(String targetUserId, Role newRole)
            throws CrobsException {
        User previous = getUser(targetUserId);
        if (previous.getRole() == Role.SUPERADMIN || newRole == Role.SUPERADMIN || newRole == null) {
            throw new CrobsException("The unique superadmin's access cannot be assigned or changed.");
        }
        User replacement = newRole == Role.ADMIN
                ? new Admin(previous.getId(), previous.getName())
                : new RegularUser(previous.getId(), previous.getName());
        replace(previous, replacement);
        return ReversibleChange.applied(replacement,
                () -> replace(replacement, previous), () -> replace(previous, replacement));
    }

    private String nextId() {
        String id;
        do {
            id = IdGenerator.generateUserId();
        } while (users.get(id) != null);
        return id;
    }

    private void requireCurrent(User expected) throws CrobsException {
        if (users.get(expected.getId()) != expected) {
            throw new CrobsException("User changed since this operation: " + expected.getId());
        }
    }

    private void requireVacant(String userId) throws CrobsException {
        if (users.get(userId) != null) {
            throw new CrobsException("User ID is already in use: " + userId);
        }
    }

    private void replace(User expected, User replacement) throws CrobsException {
        requireCurrent(expected);
        users.remove(expected.getId());
        users.add(replacement);
    }

    private static void requireCancellation(ReversibleChange<Void> cancellation) throws CrobsException {
        if (cancellation == null) {
            throw new CrobsException("Booking cancellation did not provide an undo record.");
        }
    }
}
