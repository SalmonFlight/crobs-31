package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for domain values and repository validations. */
@SuppressWarnings("MagicNumber")
class DomainValuesTest {

    /** Tests that roles follow the correct inheritance hierarchy. */
    @Test
    void userRolesFollowTheDeclaredInheritanceHierarchy() {
        User regular = new RegularUser("U001", "Alex");
        User admin = new Admin("U002", "Chris");
        User superAdmin = new SuperAdmin("U003", "Sam");
        assertEquals("U001", regular.getId());
        assertEquals("Alex", regular.getName());
        assertEquals(Role.REGULAR, regular.getRole());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.SUPERADMIN, superAdmin.getRole());
        assertTrue(superAdmin instanceof Admin);
        assertFalse(regular instanceof Admin);
    }

    /** Tests that user identity is preserved after role changes. */
    @Test
    void usersCompareByIdentityEvenAfterRoleChanges() {
        User regular = new RegularUser("U001", "Alex");
        User promoted = new Admin("U001", "Alex");
        assertEquals(regular, regular);
        assertEquals(regular, promoted);
        assertEquals(promoted, regular);
        assertEquals(regular.hashCode(), promoted.hashCode());
        assertNotEquals(new RegularUser("U002", "Alex"), regular);
        assertNotEquals(null, regular);
        assertNotEquals("U001", regular);
        assertEquals(1, new HashSet<>(Arrays.asList(regular, promoted)).size());
    }

    /** Tests that invalid user arguments are rejected. */
    @Test
    void rejectsInvalidUserConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new RegularUser(null, "Alex"));
        assertThrows(IllegalArgumentException.class, () -> new RegularUser(" ", "Alex"));
        assertThrows(IllegalArgumentException.class, () -> new RegularUser("U001", null));
        assertThrows(IllegalArgumentException.class, () -> new RegularUser("U001", " "));
        assertThrows(IllegalArgumentException.class, () -> new User("U001", "Alex", null) { });
        assertThrows(IllegalArgumentException.class, () -> new Admin("U001", "Alex", Role.REGULAR));
    }

    /** Tests that room equality uses the room ID. */
    @Test
    void roomEqualityUsesPersistentIdentity() {
        Room original = new Room("R001", RoomType.MEETING_ROOM, 10, Collections.emptySet());
        Room updated = new Room("R001", RoomType.COMPUTER_LAB, 20, Collections.singleton("screen"));
        assertEquals(original, original);
        assertEquals(original, updated);
        assertEquals(updated, original);
        assertEquals(original.hashCode(), updated.hashCode());
        assertNotEquals(new Room("R002", RoomType.MEETING_ROOM, 10, Collections.emptySet()), original);
        assertNotEquals(null, original);
        assertNotEquals("R001", original);
    }

    /** Tests that invalid room constructor values are rejected. */
    @Test
    void roomConstructorRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Room(null, RoomType.MEETING_ROOM, 10, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new Room(" ", RoomType.MEETING_ROOM, 10, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new Room("R001", null, 10, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new Room("R001", RoomType.MEETING_ROOM, 0, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new Room("R001", RoomType.MEETING_ROOM, 10, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Room("R001", RoomType.MEETING_ROOM, 10, Collections.singleton(" ")));
        assertThrows(IllegalArgumentException.class,
                () -> new Room("R001", RoomType.MEETING_ROOM, 10, Collections.singleton(null)));
    }

    /** Tests room type command spellings. */
    @Test
    void roomTypesRoundTripTheSpecifiedCommandSpellings() {
        String[] commandValues = {"lectureTheatre", "computerLab", "seminarRoom", "meetingRoom"};
        RoomType[] types = {RoomType.LECTURE_THEATRE, RoomType.COMPUTER_LAB,
                RoomType.SEMINAR_ROOM, RoomType.MEETING_ROOM};
        assertEquals(4, RoomType.values().length);
        for (int index = 0; index < types.length; index++) {
            assertSame(types[index], RoomType.fromCommandValue(commandValues[index]));
            assertEquals(commandValues[index], types[index].getCommandValue());
        }
        assertThrows(IllegalArgumentException.class, () -> RoomType.fromCommandValue(null));
        assertThrows(IllegalArgumentException.class, () -> RoomType.fromCommandValue("office"));
    }

    /** Tests the user repository. */
    @Test
    void userRepositoryRejectsDuplicateIdsAndReturnsDetachedLists() {
        UserRepository repository = new UserRepository();
        User user = new RegularUser("U001", "Alex");
        repository.add(user);
        assertSame(user, repository.get("U001"));
        assertThrows(IllegalArgumentException.class, () -> repository.add(new Admin("U001", "Other")));
        assertThrows(IllegalArgumentException.class, () -> repository.add(null));
        List<User> snapshot = repository.getAll();
        snapshot.clear();
        assertEquals(Collections.singletonList(user), repository.getAll());
        assertSame(user, repository.remove("U001"));
        assertNull(repository.get("U001"));
        assertNull(repository.remove("missing"));
        assertTrue(repository.getAll().isEmpty());
    }

    /** Tests the room repository. */
    @Test
    void roomRepositoryRejectsDuplicateIdsAndReturnsDetachedLists() {
        RoomRepository repository = new RoomRepository();
        Room room = new Room("R001", RoomType.MEETING_ROOM, 10, Collections.emptySet());
        repository.add(room);
        assertSame(room, repository.get("R001"));
        assertThrows(IllegalArgumentException.class, () -> repository.add(room));
        assertThrows(IllegalArgumentException.class, () -> repository.add(null));
        List<Room> snapshot = repository.getAll();
        snapshot.clear();
        assertEquals(Collections.singletonList(room), repository.getAll());
        assertSame(room, repository.remove("R001"));
        assertNull(repository.get("R001"));
        assertNull(repository.remove("missing"));
        assertTrue(repository.getAll().isEmpty());
    }
}