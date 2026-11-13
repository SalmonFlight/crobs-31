package hk.edu.polyu.comp.comp2021.crobs.model;

/** Authorization roles supported by CROBS. */
public enum Role {
    /** A regular user with ordinary booking permissions. */
    REGULAR,
    /** An administrator who can manage users and rooms. */
    ADMIN,
    /** The unique administrator who can manage administrator roles. */
    SUPERADMIN
}
