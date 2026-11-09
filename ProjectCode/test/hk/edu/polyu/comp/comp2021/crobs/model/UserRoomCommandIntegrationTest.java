package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.command.DeleteRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ModifyRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.RemoveUserCommand;
import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises command history together with reversible booking-domain callbacks. */
class UserRoomCommandIntegrationTest {
    private UserRepository userRepository;
    private RoomRepository roomRepository;
    private FakeBookingCancellation bookings;
    private UserService users;
    private RoomService rooms;
    private SessionContext session;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        roomRepository = new RoomRepository();
        bookings = new FakeBookingCancellation();
        users = new UserService(userRepository, bookings);
        rooms = new RoomService(roomRepository, bookings);
        session = new SessionContext(userRepository);
        session.login(users.getSuperAdmin());
    }

    @Test
    void removeUserCommandReversesItsActualBookingCascade() throws CrobsException {
        User user = users.addUser("Alex");
        bookings.add("past", user.getId(), "R001", FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", user.getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));
        RemoveUserCommand remove = new RemoveUserCommand(users);
        assertTrue(remove.execute(new String[] {"removeUser", user.getId()}, session).isSuccess());
        assertNull(userRepository.get(user.getId()));
        assertFalse(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
        remove.undo();
        assertSame(user, users.getUser(user.getId()));
        assertTrue(bookings.contains("past"));
        assertTrue(bookings.contains("future"));
        remove.redo();
        assertNull(userRepository.get(user.getId()));
        assertFalse(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
    }

    @Test
    void failedCancellationDoesNotEnterCommandHistory() throws CrobsException {
        User user = users.addUser("Alex");
        bookings.add("booking", user.getId(), "R001", FakeBookingCancellation.NOW.plusDays(1));
        bookings.failCancellation = true;
        RemoveUserCommand remove = new RemoveUserCommand(users);
        assertThrows(CrobsException.class,
                () -> remove.execute(new String[] {"removeUser", user.getId()}, session));
        assertSame(user, users.getUser(user.getId()));
        assertTrue(bookings.contains("booking"));
        assertThrows(CrobsException.class, remove::undo);
        assertThrows(CrobsException.class, remove::redo);
    }

    @Test
    void failedCascadeUndoCanBeRetriedThroughTheSameCommand() throws CrobsException {
        Room room = rooms.addRoom(RoomType.MEETING_ROOM, 10, Collections.emptySet());
        bookings.add("future", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(1));
        DeleteRoomCommand delete = new DeleteRoomCommand(rooms);
        delete.execute(new String[] {"deleteRoom", room.getId()}, session);
        bookings.failUndo = true;
        assertThrows(CrobsException.class, delete::undo);
        assertNull(roomRepository.get(room.getId()));
        assertFalse(bookings.contains("future"));
        bookings.failUndo = false;
        delete.undo();
        assertSame(room, rooms.getRoom(room.getId()));
        assertTrue(bookings.contains("future"));
        bookings.failRedo = true;
        assertThrows(CrobsException.class, delete::redo);
        assertSame(room, rooms.getRoom(room.getId()));
        assertTrue(bookings.contains("future"));
        bookings.failRedo = false;
        delete.redo();
        assertNull(roomRepository.get(room.getId()));
        assertFalse(bookings.contains("future"));
    }

    @Test
    void modifyAndDeleteRoomRestoreThePriorRoomAndFutureBookings() throws CrobsException {
        Room original = rooms.addRoom(RoomType.MEETING_ROOM, 10, Collections.singleton("screen"));
        ModifyRoomCommand modify = new ModifyRoomCommand(rooms);
        modify.execute(new String[] {"modifyRoom", original.getId(), "capacity", "30"}, session);
        bookings.add("past", "U001", original.getId(), FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", "U001", original.getId(), FakeBookingCancellation.NOW.plusDays(1));
        DeleteRoomCommand delete = new DeleteRoomCommand(rooms);
        assertTrue(delete.execute(new String[] {"deleteRoom", original.getId()}, session).isSuccess());
        assertNull(roomRepository.get(original.getId()));
        assertTrue(bookings.contains("past"));
        assertFalse(bookings.contains("future"));

        delete.undo();
        assertEquals(30, rooms.getRoom(original.getId()).getCapacity());
        assertTrue(bookings.contains("future"));
        modify.undo();
        assertSame(original, rooms.getRoom(original.getId()));
        modify.redo();
        delete.redo();
        assertNull(roomRepository.get(original.getId()));
        assertTrue(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
    }
}
