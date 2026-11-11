package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private UserRepository repository;
    private FakeBookingCancellation bookings;
    private UserService service;

    @BeforeEach
    void setUp() throws CrobsException {
        repository = new UserRepository();
        bookings = new FakeBookingCancellation();
        service = new UserService(repository, bookings);
    }

    @Test
    void startsWithExactlyOneSuperAdmin() {
        assertEquals(1, service.getAllUsers().size());
        User superAdmin = service.getSuperAdmin();
        assertTrue(superAdmin instanceof SuperAdmin);
        assertTrue(superAdmin instanceof Admin);
        assertEquals(Role.SUPERADMIN, superAdmin.getRole());
        assertEquals("Super Admin", superAdmin.getName());
        assertTrue(superAdmin.getId().matches("U\\d{3,}"));
        assertSame(superAdmin, repository.get(superAdmin.getId()));
    }

    @Test
    void reusesAnExistingSuperAdminAndRejectsMultipleSuperAdmins() {
        UserRepository prepopulated = new UserRepository();
        User existing = new SuperAdmin("U-existing", "Existing Administrator");
        prepopulated.add(new RegularUser("U-student", "Student"));
        prepopulated.add(existing);
        UserService initialized = new UserService(prepopulated, bookings);
        assertSame(existing, initialized.getSuperAdmin());
        assertEquals(2, initialized.getAllUsers().size());
        prepopulated.add(new SuperAdmin("U-other", "Other Administrator"));
        assertThrows(IllegalArgumentException.class, () -> new UserService(prepopulated, bookings));
        assertEquals(3, prepopulated.getAll().size());
    }

    @Test
    void requiresBothRepositoryAndBookingAdapter() {
        assertThrows(NullPointerException.class, () -> new UserService(null, bookings));
        assertThrows(NullPointerException.class, () -> new UserService(repository, null));
    }

    @Test
    void generatesDistinctIdsAndCreatesRegularUsers() throws CrobsException {
        Set<String> identifiers = new HashSet<>();
        identifiers.add(service.getSuperAdmin().getId());
        for (int index = 0; index < 20; index++) {
            User user = service.addUser("Student " + index);
            assertTrue(user instanceof RegularUser);
            assertEquals(Role.REGULAR, user.getRole());
            assertEquals("Student " + index, user.getName());
            assertTrue(user.getId().matches("U\\d{3,}"));
            assertTrue(identifiers.add(user.getId()), "IDs must never be reused.");
            assertSame(user, service.getUser(user.getId()));
        }
    }

    @Test
    void duplicateDisplayNamesHaveDifferentIdentities() throws CrobsException {
        User first = service.addUser("Alex");
        User second = service.addUser("Alex");
        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first, second);
    }

    @Test
    void invalidNamesAndMissingUsersLeaveStateUnchanged() {
        assertThrows(CrobsException.class, () -> service.addUser(null));
        assertThrows(CrobsException.class, () -> service.addUser("  "));
        assertThrows(CrobsException.class, () -> service.getUser("U-missing"));
        assertThrows(CrobsException.class, () -> service.removeUser("U-missing"));
        assertEquals(1, service.getAllUsers().size());
        assertEquals(0, bookings.userCancellations);
    }

    @Test
    void userListsAreDetachedFromStorage() throws CrobsException {
        User user = service.addUser("Alex");
        List<User> snapshot = service.getAllUsers();
        snapshot.clear();
        assertEquals(2, service.getAllUsers().size());
        assertSame(user, service.getUser(user.getId()));
    }

    @Test
    void removesRegularUserAndAllTheirBookings() throws CrobsException {
        User user = service.addUser("Alex");
        bookings.add("past", user.getId(), "R001", FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", user.getId(), "R002", FakeBookingCancellation.NOW.plusDays(1));
        bookings.add("other", service.getSuperAdmin().getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));

        service.removeUser(user.getId());

        assertNull(repository.get(user.getId()));
        assertFalse(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
        assertTrue(bookings.contains("other"));
        assertEquals(1, bookings.userCancellations);
    }

    @Test
    void failedCascadeKeepsUserAndBookings() throws CrobsException {
        User user = service.addUser("Alex");
        bookings.add("booking", user.getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));
        bookings.failCancellation = true;

        assertThrows(CrobsException.class, () -> service.removeUser(user.getId()));

        assertSame(user, service.getUser(user.getId()));
        assertTrue(bookings.contains("booking"));
    }

    @Test
    void missingCancellationRecordDoesNotDeleteUser() throws CrobsException {
        User user = service.addUser("Alex");
        bookings.returnNullCancellation = true;
        assertThrows(CrobsException.class, () -> service.removeUser(user.getId()));
        assertSame(user, service.getUser(user.getId()));
    }

    @Test
    void failedBookingRestorationRollsBackUserRestorationAndCanBeRetried() throws CrobsException {
        User user = service.addUser("Alex");
        bookings.add("booking", user.getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));
        ReversibleChange<User> deletion = service.removeUserWithUndo(user.getId());
        bookings.failUndo = true;
        assertThrows(CrobsException.class, deletion::undo);
        assertNull(repository.get(user.getId()));
        assertFalse(bookings.contains("booking"));
        bookings.failUndo = false;
        deletion.undo();
        assertSame(user, service.getUser(user.getId()));
        assertTrue(bookings.contains("booking"));
        bookings.failRedo = true;
        assertThrows(CrobsException.class, deletion::redo);
        assertSame(user, service.getUser(user.getId()));
        assertTrue(bookings.contains("booking"));
        bookings.failRedo = false;
        deletion.redo();
        assertNull(repository.get(user.getId()));
        assertFalse(bookings.contains("booking"));
    }

    @Test
    void promotionAndDemotionPreserveIdentityAndName() throws CrobsException {
        User user = service.addUser("Alex");
        service.changeRole(user.getId(), Role.ADMIN);
        User promoted = service.getUser(user.getId());
        assertTrue(promoted instanceof Admin);
        assertEquals(Role.ADMIN, promoted.getRole());
        assertEquals(user.getId(), promoted.getId());
        assertEquals(user.getName(), promoted.getName());
        assertEquals(user, promoted);
        assertEquals(user.hashCode(), promoted.hashCode());

        service.changeRole(user.getId(), Role.REGULAR);
        assertTrue(service.getUser(user.getId()) instanceof RegularUser);
        assertEquals(Role.REGULAR, service.getUser(user.getId()).getRole());
    }

    @Test
    void administratorsCannotBeRemovedUntilDemoted() throws CrobsException {
        User user = service.addUser("Alex");
        service.changeRole(user.getId(), Role.ADMIN);
        assertThrows(CrobsException.class, () -> service.removeUser(user.getId()));
        assertEquals(0, bookings.userCancellations);
        assertEquals(Role.ADMIN, service.getUser(user.getId()).getRole());

        service.changeRole(user.getId(), Role.REGULAR);
        service.removeUser(user.getId());
        assertNull(repository.get(user.getId()));
    }

    @Test
    void protectsTheUniqueSuperAdminAndRejectsInvalidRoleChanges() throws CrobsException {
        User superAdmin = service.getSuperAdmin();
        User user = service.addUser("Alex");
        assertThrows(CrobsException.class, () -> service.removeUser(superAdmin.getId()));
        assertThrows(CrobsException.class, () -> service.changeRole(superAdmin.getId(), Role.REGULAR));
        assertThrows(CrobsException.class, () -> service.changeRole(superAdmin.getId(), Role.ADMIN));
        assertThrows(CrobsException.class, () -> service.changeRole(user.getId(), Role.SUPERADMIN));
        assertThrows(CrobsException.class, () -> service.changeRole(user.getId(), null));
        assertThrows(CrobsException.class, () -> service.changeRole("missing", Role.ADMIN));
        assertSame(superAdmin, service.getSuperAdmin());
        assertEquals(Role.REGULAR, service.getUser(user.getId()).getRole());
        assertEquals(1, service.getAllUsers().stream()
                .filter(current -> current.getRole() == Role.SUPERADMIN).count());
    }

    @Test
    void undoAndRedoAdditionReuseTheOriginalId() throws CrobsException {
        ReversibleChange<User> change = service.addUserWithUndo("Alex");
        User original = change.getValue();
        assertSame(original, service.getUser(original.getId()));
        change.undo();
        assertNull(repository.get(original.getId()));
        change.redo();
        assertSame(original, service.getUser(original.getId()));
        User next = service.addUser("Next");
        assertNotEquals(original.getId(), next.getId());
    }

    @Test
    void undoAndRedoRemovalRestoreExactCascadeSnapshot() throws CrobsException {
        User user = service.addUser("Alex");
        bookings.add("past", user.getId(), "R001", FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", user.getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));
        ReversibleChange<User> change = service.removeUserWithUndo(user.getId());
        assertEquals(user, change.getValue());
        assertFalse(bookings.contains("past"));
        assertFalse(bookings.contains("future"));

        change.undo();
        assertSame(user, service.getUser(user.getId()));
        assertTrue(bookings.contains("past"));
        assertTrue(bookings.contains("future"));
        bookings.add("later", user.getId(), "R002", FakeBookingCancellation.NOW.plusDays(2));

        change.redo();
        assertNull(repository.get(user.getId()));
        assertFalse(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
        assertTrue(bookings.contains("later"), "Redo must replay the original cancellation snapshot.");
        assertEquals(1, bookings.userCancellations, "Redo uses the saved cancellation action.");
    }

    @Test
    void undoAndRedoRoleChangesRestoreTheCorrectSubclasses() throws CrobsException {
        User user = service.addUser("Alex");
        ReversibleChange<User> change = service.changeRoleWithUndo(user.getId(), Role.ADMIN);
        assertTrue(service.getUser(user.getId()) instanceof Admin);
        change.undo();
        assertSame(user, service.getUser(user.getId()));
        change.redo();
        assertTrue(service.getUser(user.getId()) instanceof Admin);
        assertEquals(user.getId(), service.getUser(user.getId()).getId());
    }
}
