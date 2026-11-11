package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionContextTest {
    @Test
    void startsLoggedOutAndChecksRolePermissions() throws CrobsException {
        SessionContext session = new SessionContext();
        assertNull(session.getCurrentUser());
        assertThrows(CrobsException.class, session::requireLoggedIn);
        assertThrows(CrobsException.class, session::requireAdmin);
        assertThrows(CrobsException.class, session::requireSuperAdmin);

        User regular = new RegularUser("U001", "Student");
        session.login(regular);
        assertSame(regular, session.requireLoggedIn());
        assertThrows(CrobsException.class, session::requireAdmin);
        assertThrows(CrobsException.class, session::requireSuperAdmin);

        User admin = new Admin("U002", "Administrator");
        session.login(admin);
        assertSame(admin, session.requireAdmin());
        assertThrows(CrobsException.class, session::requireSuperAdmin);

        User superAdmin = new SuperAdmin("U003", "Super Admin");
        session.login(superAdmin);
        assertSame(superAdmin, session.requireAdmin());
        assertSame(superAdmin, session.requireSuperAdmin());
        session.logout();
        assertNull(session.getCurrentUser());
    }

    @Test
    void observesRoleChangesInAnExistingSession() throws CrobsException {
        UserRepository repository = new UserRepository();
        UserService users = new UserService(repository, new FakeBookingCancellation());
        User user = users.addUser("Alex");
        SessionContext session = new SessionContext(repository);
        session.login(user);
        users.changeRole(user.getId(), Role.ADMIN);
        assertSame(users.getUser(user.getId()), session.requireAdmin());
        users.changeRole(user.getId(), Role.REGULAR);
        assertThrows(CrobsException.class, session::requireAdmin);
        assertEquals(Role.REGULAR, session.getCurrentUser().getRole());
    }

    @Test
    void deletedUserCannotKeepAnAuthenticatedSession() throws CrobsException {
        UserRepository repository = new UserRepository();
        UserService users = new UserService(repository, new FakeBookingCancellation());
        User user = users.addUser("Alex");
        SessionContext session = new SessionContext(repository);
        session.login(user);
        users.removeUser(user.getId());
        assertNull(session.getCurrentUser());
        assertThrows(CrobsException.class, session::requireLoggedIn);
        assertThrows(CrobsException.class, session::requireAdmin);
    }

    @Test
    void rejectsNullLoginWithoutClearingAnExistingLogin() {
        SessionContext session = new SessionContext();
        User user = new RegularUser("U001", "Alex");
        session.login(user);
        assertThrows(NullPointerException.class, () -> session.login(null));
        assertSame(user, session.getCurrentUser());
    }
}
