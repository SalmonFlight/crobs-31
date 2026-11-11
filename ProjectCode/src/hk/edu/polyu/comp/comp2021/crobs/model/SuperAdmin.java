package hk.edu.polyu.comp.comp2021.crobs.model;

/** The administrator who can assign and revoke administrator roles. */
public final class SuperAdmin extends Admin {
    /**
     * Creates a super administrator.
     *
     * @param id the user identifier
     * @param name the display name
     */
    public SuperAdmin(String id, String name) {
        super(id, name, Role.SUPERADMIN);
    }
}
