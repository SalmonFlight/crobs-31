package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user with room and user administration permissions. */
public class Admin extends User {
    /**
     * Creates an administrator.
     *
     * @param id the user identifier
     * @param name the display name
     */
    public Admin(String id, String name) {
        this(id, name, Role.ADMIN);
    }

    /**
     * Creates an administrator or super administrator.
     *
     * @param id the user identifier
     * @param name the display name
     * @param role ADMIN or SUPERADMIN
     */
    protected Admin(String id, String name, Role role) {
        super(id, name, role);
        if (role != Role.ADMIN && role != Role.SUPERADMIN) {
            throw new IllegalArgumentException("An administrator must have an administrator role.");
        }
    }
}
