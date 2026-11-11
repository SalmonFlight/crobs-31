package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Objects;

/** A registered user whose identity stays the same when their role changes. */
public abstract class User {
    private final String id;
    private final String name;
    private final Role role;

    /**
     * Creates a user.
     *
     * @param id the nonblank user identifier
     * @param name the nonblank display name
     * @param role the user's role
     */
    protected User(String id, String name, Role role) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name must not be blank.");
        }
        if (role == null) {
            throw new IllegalArgumentException("User role is required.");
        }
        this.id = id;
        this.name = name;
        this.role = role;
    }

    /** @return the user identifier */
    public final String getId() {
        return id;
    }

    /** @return the display name */
    public final String getName() {
        return name;
    }

    /** @return the user's current role */
    public final Role getRole() {
        return role;
    }

    /**
     * Compares persistent identity, including across role-specific subclasses.
     *
     * @param other the object to compare
     * @return whether both objects represent the same user
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User user = (User) other;
        return id.equals(user.getId());
    }

    /** @return the hash code of the user identifier */
    @Override
    public final int hashCode() {
        return Objects.hashCode(id);
    }
}
