package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user with ordinary booking permissions. */
public final class RegularUser extends User {
    /**
     * Creates a regular user.
     *
     * @param id the user identifier
     * @param name the display name
     */
    public RegularUser(String id, String name) {
        super(id, name, Role.REGULAR);
    }
}
