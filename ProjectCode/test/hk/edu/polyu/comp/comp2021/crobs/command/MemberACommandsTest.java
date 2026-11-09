package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Admin;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingCancellation;
import hk.edu.polyu.comp.comp2021.crobs.model.RegularUser;
import hk.edu.polyu.comp.comp2021.crobs.model.ReversibleChange;
import hk.edu.polyu.comp.comp2021.crobs.model.Role;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;
import org.junit.jupiter.api.BeforeEach;
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

/** Tests Member A's commands independently of Member B's booking implementation. */
@SuppressWarnings("MagicNumber") // Literal capacities and token counts are test inputs.
class MemberACommandsTest {
    private UserRepository users;
    private RoomRepository rooms;
    private UserService userService;
    private RoomService roomService;
    private SessionContext session;

    /** Creates isolated repositories and an explicit booking cancellation fake. */
    @BeforeEach
    void setUp() {
        users = new UserRepository();
        rooms = new RoomRepository();
        BookingCancellation bookings = new BookingCancellation() {
            @Override
            public ReversibleChange<Void> cancelBookingsByUser(String userId) {
                return ReversibleChange.applied(null, () -> { }, () -> { });
            }

            @Override
            public ReversibleChange<Void> cancelFutureBookingsForRoom(String roomId) {
                return ReversibleChange.applied(null, () -> { }, () -> { });
            }
        };
        userService = new UserService(users, bookings);
        roomService = new RoomService(rooms, bookings);
        session = new SessionContext(users);
    }

    /** Verifies the names used by the dispatcher. */
    @Test
    void namesMatchTheRequiredCommandSpelling() {
        List<Command> commands = Arrays.asList(new LoginCommand(userService), new LogoutCommand(),
                new AddUserCommand(userService), new RemoveUserCommand(userService),
                new ChangeRoleCommand(userService), new AddRoomCommand(roomService),
                new ModifyRoomCommand(roomService), new DeleteRoomCommand(roomService));
        List<String> expected = Arrays.asList("login", "logout", "addUser", "removeUser",
                "changeRole", "addRoom", "modifyRoom", "deleteRoom");
        for (int i = 0; i < commands.size(); i++) {
            assertEquals(expected.get(i), commands.get(i).getName());
        }
    }

    /**
     * Checks login/logout behavior and BON2 exclusions.
     * @throws CrobsException when an expected-success command fails
     */
    @Test
    void loginAndLogoutChangeSessionButDoNotParticipateInUndo() throws CrobsException {
        assertNull(session.getCurrentUser());
        User user = userService.addUser("Alice");
        LoginCommand login = new LoginCommand(userService);
        CommandResult result = login.execute(new String[]{"login", user.getId()}, session);
        assertTrue(result.isSuccess());
        assertSame(user, result.getPayload());
        assertSame(user, session.getCurrentUser());
        login.undo();
        login.redo();
        assertSame(user, session.getCurrentUser());

        assertThrows(CrobsException.class,
                () -> login.execute(new String[]{"login", "missing"}, session));
        assertSame(user, session.getCurrentUser());

        LogoutCommand logout = new LogoutCommand();
        assertTrue(logout.execute(new String[]{"logout"}, session).isSuccess());
        logout.undo();
        logout.redo();
        assertNull(session.getCurrentUser());
        assertTrue(logout.execute(new String[]{"logout"}, session).isSuccess());
    }

    /**
     * Checks that login can switch the active user.
     * @throws CrobsException when an expected-success command fails
     */
    @Test
    void loginCanSwitchBetweenExistingUsers() throws CrobsException {
        LoginCommand login = new LoginCommand(userService);
        User user = userService.addUser("Alice");
        login.execute(new String[]{"login", userService.getSuperAdmin().getId()}, session);
        login.execute(new String[]{"login", user.getId()}, session);
        assertSame(user, session.getCurrentUser());
    }

    /** Checks authentication command argument validation. */
    @Test
    void authCommandsRejectMalformedArguments() {
        LoginCommand login = new LoginCommand(userService);
        LogoutCommand logout = new LogoutCommand();
        assertThrows(CrobsException.class, () -> login.execute(null, session));
        assertThrows(CrobsException.class, () -> login.execute(new String[0], session));
        assertThrows(CrobsException.class, () -> login.execute(new String[]{"login"}, session));
        assertThrows(CrobsException.class,
                () -> login.execute(new String[]{"login", "id", "extra"}, session));
        assertThrows(CrobsException.class,
                () -> login.execute(new String[]{"other", "id"}, session));
        assertThrows(CrobsException.class,
                () -> logout.execute(new String[]{"logout", "extra"}, session));
        assertThrows(CrobsException.class, () -> logout.execute(null, session));
        assertThrows(CrobsException.class, () -> login.execute(new String[]{"login", null}, session));
        assertThrows(CrobsException.class, () -> login.execute(new String[]{"login", " "}, session));
    }

    /**
     * Checks that all mutations reject logged-out and regular users.
     * @throws CrobsException when fixture creation fails
     */
    @Test
    void everyMutationRequiresLoginAndRegularUsersCannotMutate() throws CrobsException {
        User regular = userService.addUser("Regular");
        Room room = roomService.addRoom(RoomType.MEETING_ROOM, 20, Collections.emptySet());
        List<Command> commands = Arrays.asList(new AddUserCommand(userService),
                new RemoveUserCommand(userService), new ChangeRoleCommand(userService),
                new AddRoomCommand(roomService), new ModifyRoomCommand(roomService),
                new DeleteRoomCommand(roomService));
        List<String[]> arguments = Arrays.asList(new String[]{"addUser", "New"},
                new String[]{"removeUser", regular.getId()},
                new String[]{"changeRole", regular.getId(), "admin"},
                new String[]{"addRoom", "meetingRoom", "10", "-"},
                new String[]{"modifyRoom", room.getId(), "capacity", "40"},
                new String[]{"deleteRoom", room.getId()});
        for (int i = 0; i < commands.size(); i++) {
            final Command command = commands.get(i);
            final String[] args = arguments.get(i);
            assertThrows(CrobsException.class, () -> command.execute(args, session));
        }
        session.login(regular);
        for (int i = 0; i < commands.size(); i++) {
            final Command command = commands.get(i);
            final String[] args = arguments.get(i);
            assertThrows(CrobsException.class, () -> command.execute(args, session));
        }
        assertEquals(2, users.getAll().size());
        assertSame(room, rooms.get(room.getId()));
    }

    /**
     * Checks the administrator's permitted operations.
     * @throws CrobsException when an expected-success command fails
     */
    @Test
    void adminCanManageUsersAndRoomsButCannotChangeRoles() throws CrobsException {
        User admin = userService.addUser("Admin");
        userService.changeRole(admin.getId(), Role.ADMIN);
        session.login(userService.getUser(admin.getId()));
        AddUserCommand addUser = new AddUserCommand(userService);
        User target = (User) addUser.execute(new String[]{"addUser", "Alice", "Chan"}, session).getPayload();
        assertEquals("Alice Chan", target.getName());
        assertEquals(Role.REGULAR, target.getRole());
        assertThrows(CrobsException.class, () -> new ChangeRoleCommand(userService)
                .execute(new String[]{"changeRole", target.getId(), "admin"}, session));

        Room room = (Room) new AddRoomCommand(roomService)
                .execute(new String[]{"addRoom", "computerLab", "30", "projector,computers"}, session)
                .getPayload();
        assertEquals(RoomType.COMPUTER_LAB, room.getType());
        assertEquals(new HashSet<>(Arrays.asList("projector", "computers")), room.getFeatures());
        new ModifyRoomCommand(roomService)
                .execute(new String[]{"modifyRoom", room.getId(), "capacity", "40"}, session);
        assertEquals(40, roomService.getRoom(room.getId()).getCapacity());
        new DeleteRoomCommand(roomService).execute(new String[]{"deleteRoom", room.getId()}, session);
        new RemoveUserCommand(userService).execute(new String[]{"removeUser", target.getId()}, session);
        assertNull(rooms.get(room.getId()));
        assertNull(users.get(target.getId()));
    }

    /**
     * Checks role changes, preservation of user identity, and superadmin restrictions.
     * @throws CrobsException when an expected-success command fails
     */
    @Test
    void onlyOtherUsersMayChangeBetweenRegularAndAdminRoles() throws CrobsException {
        session.login(userService.getSuperAdmin());
        User user = userService.addUser("Alice");
        ChangeRoleCommand command = new ChangeRoleCommand(userService);
        User promoted = (User) command.execute(new String[]{"changeRole", user.getId(), "ADMIN"}, session)
                .getPayload();
        assertTrue(promoted instanceof Admin);
        assertEquals(user.getId(), promoted.getId());
        assertEquals(user.getName(), promoted.getName());
        command.undo();
        assertSame(user, userService.getUser(user.getId()));
        command.redo();
        assertSame(promoted, userService.getUser(user.getId()));
        command.execute(new String[]{"changeRole", user.getId(), "regularUser"}, session);
        assertTrue(userService.getUser(user.getId()) instanceof RegularUser);
        assertThrows(CrobsException.class, () -> command.execute(
                new String[]{"changeRole", userService.getSuperAdmin().getId(), "regular"}, session));
        assertThrows(CrobsException.class,
                () -> command.execute(new String[]{"changeRole", user.getId(), "superadmin"}, session));
        assertThrows(CrobsException.class,
                () -> command.execute(new String[]{"changeRole", user.getId(), "owner"}, session));
    }

    /**
     * Checks histories when one registered command handles multiple invocations.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void reusedAddCommandUndoesEveryInvocationAndRedoesTheOriginalIds() throws CrobsException {
        session.login(userService.getSuperAdmin());
        AddUserCommand command = new AddUserCommand(userService);
        User first = (User) command.execute(new String[]{"addUser", "First"}, session).getPayload();
        User second = (User) command.execute(new String[]{"addUser", "Second"}, session).getPayload();
        assertNotEquals(first.getId(), second.getId());
        command.undo();
        assertNull(users.get(second.getId()));
        assertSame(first, users.get(first.getId()));
        command.undo();
        assertNull(users.get(first.getId()));
        command.redo();
        assertSame(first, users.get(first.getId()));
        command.redo();
        assertSame(second, users.get(second.getId()));
    }

    /**
     * Checks redo preservation after failure and invalidation after a new mutation.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void failedExecutionPreservesRedoAndSuccessfulExecutionClearsIt() throws CrobsException {
        session.login(userService.getSuperAdmin());
        AddUserCommand command = new AddUserCommand(userService);
        User first = (User) command.execute(new String[]{"addUser", "First"}, session).getPayload();
        command.undo();
        assertThrows(CrobsException.class, () -> command.execute(new String[]{"addUser"}, session));
        command.redo();
        assertSame(first, users.get(first.getId()));
        command.undo();
        User second = (User) command.execute(new String[]{"addUser", "Second"}, session).getPayload();
        assertThrows(CrobsException.class, command::redo);
        assertNull(users.get(first.getId()));
        assertSame(second, users.get(second.getId()));
    }

    /**
     * Checks that history remains retryable after conflicting repository changes.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void emptyHistoryAndFailedUndoOrRedoKeepTheRemainingHistory() throws CrobsException {
        session.login(userService.getSuperAdmin());
        AddUserCommand command = new AddUserCommand(userService);
        assertThrows(CrobsException.class, command::undo);
        assertThrows(CrobsException.class, command::redo);
        User original = (User) command.execute(new String[]{"addUser", "Original"}, session).getPayload();
        users.remove(original.getId());
        users.add(new RegularUser(original.getId(), "External replacement"));
        assertThrows(CrobsException.class, command::undo);
        users.remove(original.getId());
        users.add(original);
        command.undo();
        users.add(new RegularUser(original.getId(), "Conflicting ID"));
        assertThrows(CrobsException.class, command::redo);
        users.remove(original.getId());
        command.redo();
        assertSame(original, users.get(original.getId()));
    }

    /**
     * Checks reversible user deletion and administrator protection.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void removalRestoresTheSameUserOnUndoAndRemovesItOnRedo() throws CrobsException {
        session.login(userService.getSuperAdmin());
        User user = userService.addUser("Alice");
        RemoveUserCommand command = new RemoveUserCommand(userService);
        assertSame(user, command.execute(new String[]{"removeUser", user.getId()}, session).getPayload());
        assertNull(users.get(user.getId()));
        command.undo();
        assertSame(user, users.get(user.getId()));
        command.redo();
        assertNull(users.get(user.getId()));
        assertThrows(CrobsException.class, () -> command.execute(
                new String[]{"removeUser", userService.getSuperAdmin().getId()}, session));
    }

    /**
     * Checks interleaved room command histories in global LIFO order.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void roomCommandsInterleaveAndReverseInGlobalExecutionOrder() throws CrobsException {
        session.login(userService.getSuperAdmin());
        AddRoomCommand add = new AddRoomCommand(roomService);
        ModifyRoomCommand modify = new ModifyRoomCommand(roomService);
        DeleteRoomCommand delete = new DeleteRoomCommand(roomService);
        Room original = (Room) add.execute(new String[]{"addRoom", "meetingRoom", "10", "-"}, session)
                .getPayload();
        assertTrue(original.getFeatures().isEmpty());
        Room modified = (Room) modify.execute(
                new String[]{"modifyRoom", original.getId(), "capacity", "20"}, session).getPayload();
        delete.execute(new String[]{"deleteRoom", original.getId()}, session);
        assertNull(rooms.get(original.getId()));
        delete.undo();
        assertSame(modified, rooms.get(original.getId()));
        modify.undo();
        assertSame(original, rooms.get(original.getId()));
        add.undo();
        assertNull(rooms.get(original.getId()));
        add.redo();
        modify.redo();
        delete.redo();
        assertNull(rooms.get(original.getId()));
    }

    /**
     * Checks room field parsing and preservation of untouched values.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void modifyCommandAcceptsEachFieldAndPreservesUnmodifiedFields() throws CrobsException {
        session.login(userService.getSuperAdmin());
        Room original = roomService.addRoom(RoomType.MEETING_ROOM, 10, Collections.singleton("projector"));
        ModifyRoomCommand command = new ModifyRoomCommand(roomService);
        command.execute(new String[]{"modifyRoom", original.getId(), "roomType", "lectureTheatre"}, session);
        assertEquals(RoomType.LECTURE_THEATRE, roomService.getRoom(original.getId()).getType());
        command.execute(new String[]{"modifyRoom", original.getId(), "features", "whiteboard,projector"}, session);
        Room changed = roomService.getRoom(original.getId());
        assertEquals(10, changed.getCapacity());
        assertEquals(2, changed.getFeatures().size());
        command.undo();
        assertEquals(Collections.singleton("projector"), roomService.getRoom(original.getId()).getFeatures());
        command.undo();
        assertSame(original, roomService.getRoom(original.getId()));
        command.redo();
        command.redo();
        assertSame(changed, roomService.getRoom(original.getId()));
    }

    /** Checks that invalid room inputs never create a room. */
    @Test
    void roomCreationRejectsBadTypesCapacitiesFeaturesAndArgumentCounts() {
        session.login(userService.getSuperAdmin());
        AddRoomCommand command = new AddRoomCommand(roomService);
        List<String[]> invalid = Arrays.asList(
                new String[]{"addRoom", "unknown", "10", "-"},
                new String[]{"addRoom", "meetingRoom", "ten", "-"},
                new String[]{"addRoom", "meetingRoom", "2147483648", "-"},
                new String[]{"addRoom", "meetingRoom", "0", "-"},
                new String[]{"addRoom", "meetingRoom", "-1", "-"},
                new String[]{"addRoom", "meetingRoom", "10", "projector,"},
                new String[]{"addRoom", "meetingRoom", "10", ",projector"},
                new String[]{"addRoom", "meetingRoom", "10", "white board"},
                new String[]{"addRoom", "meetingRoom", "10", "projector,,whiteboard"},
                new String[]{"addRoom", "meetingRoom", "10"},
                new String[]{"addRoom", "meetingRoom", "10", "-", "extra"});
        for (String[] args : invalid) {
            assertThrows(CrobsException.class, () -> command.execute(args, session));
        }
        assertTrue(rooms.getAll().isEmpty());
    }

    /**
     * Checks that invalid arguments and unknown IDs cannot mutate stored entities.
     * @throws CrobsException when fixture creation fails
     */
    @Test
    void malformedOrUnknownTargetsDoNotMutateAnything() throws CrobsException {
        session.login(userService.getSuperAdmin());
        User user = userService.addUser("Alice");
        Room room = roomService.addRoom(RoomType.SEMINAR_ROOM, 10, Collections.emptySet());
        assertThrows(CrobsException.class, () -> new AddUserCommand(userService)
                .execute(new String[]{"addUser", " "}, session));
        assertThrows(CrobsException.class, () -> new RemoveUserCommand(userService)
                .execute(new String[]{"removeUser", user.getId(), "extra"}, session));
        assertThrows(CrobsException.class, () -> new RemoveUserCommand(userService)
                .execute(new String[]{"removeUser", "missing"}, session));
        assertThrows(CrobsException.class, () -> new ChangeRoleCommand(userService)
                .execute(new String[]{"changeRole", user.getId()}, session));
        assertThrows(CrobsException.class, () -> new DeleteRoomCommand(roomService)
                .execute(new String[]{"deleteRoom"}, session));
        assertThrows(CrobsException.class, () -> new DeleteRoomCommand(roomService)
                .execute(new String[]{"deleteRoom", "missing"}, session));
        assertThrows(CrobsException.class, () -> new ModifyRoomCommand(roomService)
                .execute(new String[]{"modifyRoom", room.getId(), "id", "replacement"}, session));
        assertThrows(CrobsException.class, () -> new ModifyRoomCommand(roomService)
                .execute(new String[]{"modifyRoom", room.getId(), "capacity", "0"}, session));
        assertSame(user, users.get(user.getId()));
        assertSame(room, rooms.get(room.getId()));
    }

    /**
     * Checks that authorization resolves current repository state.
     * @throws CrobsException when an expected-success operation fails
     */
    @Test
    void permissionsObserveRoleChangesAndDeletedUsersImmediately() throws CrobsException {
        User user = userService.addUser("Alice");
        SessionContext otherSession = new SessionContext(users);
        otherSession.login(user);
        AddUserCommand add = new AddUserCommand(userService);
        assertThrows(CrobsException.class,
                () -> add.execute(new String[]{"addUser", "Before promotion"}, otherSession));
        userService.changeRole(user.getId(), Role.ADMIN);
        assertTrue(add.execute(new String[]{"addUser", "After promotion"}, otherSession).isSuccess());
        userService.changeRole(user.getId(), Role.REGULAR);
        assertThrows(CrobsException.class,
                () -> add.execute(new String[]{"addUser", "After demotion"}, otherSession));
        userService.removeUser(user.getId());
        assertNull(otherSession.getCurrentUser());
        assertThrows(CrobsException.class, otherSession::requireLoggedIn);
        assertFalse(userService.getAllUsers().isEmpty());
    }
}
