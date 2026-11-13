//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package test.hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.command.*;
import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.ReversibleChange;
import hk.edu.polyu.comp.comp2021.crobs.model.Role;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberBCommandsTest {
    private static final String DAY = "2026-11-16";
    private BookingService bookingService;
    private UserService userService;
    private RoomService roomService;
    private SessionContext session;
    private User alice;
    private User bob;
    private User admin;
    private String room;
    private String otherRoom;
    private BookCommand book;
    private ModifyBookingCommand modify;
    private DeleteBookingCommand delete;
    private ScheduleCommand schedule;

    MemberBCommandsTest() {
    }

    @BeforeEach
    void setUp() throws CrobsException {
        UserRepository users = new UserRepository();
        RoomRepository rooms = new RoomRepository();
        this.bookingService = new BookingService(new BookingRepository(), rooms, Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC));
        this.userService = new UserService(users, this.bookingService);
        this.roomService = new RoomService(rooms, this.bookingService);
        this.session = new SessionContext(users);
        this.alice = this.userService.addUser("Alice");
        this.bob = this.userService.addUser("Bob");
        this.admin = this.userService.getSuperAdmin();
        this.room = this.roomService.addRoom(RoomType.MEETING_ROOM, 10, Collections.emptySet()).getId();
        this.otherRoom = this.roomService.addRoom(RoomType.SEMINAR_ROOM, 30, Collections.emptySet()).getId();
        this.book = new BookCommand(this.bookingService);
        this.modify = new ModifyBookingCommand(this.bookingService);
        this.delete = new DeleteBookingCommand(this.bookingService);
        this.schedule = new ScheduleCommand(this.bookingService);
        this.session.login(this.alice);
    }

    private static String[] tokens(String line) {
        return line.split(" ");
    }

    private CommandResult run(Command command, String line) throws CrobsException {
        return command.execute(tokens(line), this.session);
    }

    private Booking bookAsAlice(String start, String end) throws CrobsException {
        return (Booking)this.run(this.book, "book " + this.room + " " + "2026-11-16" + " " + start + " " + end + " Study").getPayload();
    }

    @Test
    void namesMatchTheRequiredCommandSpelling() {
        List<Command> commands = Arrays.asList(this.book, this.modify, this.delete, this.schedule);
        List<String> expected = Arrays.asList("book", "modifyBooking", "deleteBooking", "schedule");

        for(int i = 0; i < commands.size(); ++i) {
            Assertions.assertEquals(expected.get(i), ((Command)commands.get(i)).getName());
        }

    }

    @Test
    void everyCommandRequiresLogin() {
        this.session.logout();
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 09:00 10:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.modify, "modifyBooking B001 " + this.room + " " + "2026-11-16" + " 09:00 10:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.delete, "deleteBooking B001"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.schedule, "schedule " + this.room + " " + "2026-11-16"));
    }

    @Test
    void bookCreatesABookingForTheLoggedInUser() throws CrobsException {
        CommandResult result = this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 09:00 10:30 Group study session");
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().startsWith("Booked "));
        Booking booking = (Booking)result.getPayload();
        Assertions.assertEquals(this.alice.getId(), booking.getUserId());
        Assertions.assertEquals("Group study session", booking.getPurpose());
        Assertions.assertEquals(1, this.bookingService.getBookingsByUser(this.alice.getId()).size());
    }

    @Test
    void bookRejectsMalformedInput() throws CrobsException {
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 09:00 10:00"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " 2026-13-45 09:00 10:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " 16/11/2026 09:00 10:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 9:00 10:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 25:00 26:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 10:00 09:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book R999 2026-11-16 09:00 10:00 x"));
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
    }

    @Test
    void bookRejectsConflicts() throws CrobsException {
        this.bookAsAlice("09:00", "11:00");
        this.session.login(this.bob);
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 10:00 12:00 x"));
        Assertions.assertTrue(this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 11:00 12:00 x").isSuccess());
    }

    @Test
    void modifyBookingUpdatesTheBooking() throws CrobsException {
        Booking original = this.bookAsAlice("09:00", "10:00");
        CommandResult result = this.run(this.modify, "modifyBooking " + original.getId() + " " + this.otherRoom + " 2026-11-17 14:00 15:00 Moved to B");
        Assertions.assertTrue(result.isSuccess());
        Booking updated = (Booking)result.getPayload();
        Assertions.assertEquals(original.getId(), updated.getId());
        Assertions.assertEquals(this.otherRoom, updated.getRoomId());
        Assertions.assertEquals("Moved to B", updated.getPurpose());
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.modify, "modifyBooking " + original.getId()));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.modify, "modifyBooking " + original.getId() + " " + this.room + " " + "2026-11-16" + " bad 10:00 x"));
    }

    @Test
    void onlyTheOwnerMayModifyOrDelete() throws CrobsException {
        Booking original = this.bookAsAlice("09:00", "10:00");
        this.session.login(this.bob);
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.modify, "modifyBooking " + original.getId() + " " + this.room + " " + "2026-11-16" + " 13:00 14:00 x"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.delete, "deleteBooking " + original.getId()));
        this.session.login(this.admin);
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.delete, "deleteBooking " + original.getId()));
        Assertions.assertEquals(1, this.bookingService.getBookingsByUser(this.alice.getId()).size());
    }

    @Test
    void deleteBookingRemovesTheBooking() throws CrobsException {
        Booking original = this.bookAsAlice("09:00", "10:00");
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.delete, "deleteBooking"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.delete, "deleteBooking B999"));
        Assertions.assertTrue(this.run(this.delete, "deleteBooking " + original.getId()).isSuccess());
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
    }

    @Test
    void bookingCommandsSupportUndoAndRedo() throws CrobsException {
        Booking created = this.bookAsAlice("09:00", "10:00");
        this.book.undo();
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
        this.book.redo();
        Assertions.assertEquals(created.getId(), ((Booking)this.bookingService.getBookingsByUser(this.alice.getId()).get(0)).getId());
        this.run(this.modify, "modifyBooking " + created.getId() + " " + this.otherRoom + " " + "2026-11-16" + " 13:00 14:00 x");
        this.modify.undo();
        Assertions.assertEquals(this.room, this.bookingService.getBooking(created.getId()).getRoomId());
        this.modify.redo();
        Assertions.assertEquals(this.otherRoom, this.bookingService.getBooking(created.getId()).getRoomId());
        this.run(this.delete, "deleteBooking " + created.getId());
        this.delete.undo();
        Assertions.assertEquals(1, this.bookingService.getBookingsByUser(this.alice.getId()).size());
        this.delete.redo();
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
    }

    @Test
    void undoAndRedoWithNothingRecordedFail() {
        BookCommand var10001 = this.book;
        Objects.requireNonNull(var10001);
        Assertions.assertThrows(CrobsException.class, var10001::undo);
        var10001 = this.book;
        Objects.requireNonNull(var10001);
        Assertions.assertThrows(CrobsException.class, var10001::redo);
    }

    @Test
    void scheduleHidesOtherUsersFromRegularUsers() throws CrobsException {
        this.bookAsAlice("09:00", "10:00");
        this.session.login(this.bob);
        this.run(this.book, "book " + this.room + " " + "2026-11-16" + " 10:00 11:00 Bobs");
        CommandResult asBob = this.run(this.schedule, "schedule " + this.room + " " + "2026-11-16");
        Assertions.assertTrue(asBob.getMessage().contains("user " + this.bob.getId()));
        Assertions.assertTrue(asBob.getMessage().contains("user hidden"));
        Assertions.assertFalse(asBob.getMessage().contains("user " + this.alice.getId()));
        this.session.login(this.admin);
        CommandResult asAdmin = this.run(this.schedule, "schedule " + this.room + " " + "2026-11-16");
        Assertions.assertTrue(asAdmin.getMessage().contains("user " + this.alice.getId()));
        Assertions.assertTrue(asAdmin.getMessage().contains("user " + this.bob.getId()));
        Assertions.assertFalse(asAdmin.getMessage().contains("hidden"));
    }

    @Test
    void scheduleHandlesEmptyWeeksAndBadInput() throws CrobsException {
        CommandResult empty = this.run(this.schedule, "schedule " + this.room + " " + "2026-11-16");
        Assertions.assertTrue(empty.isSuccess());
        Assertions.assertTrue(empty.getMessage().startsWith("No bookings"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.schedule, "schedule " + this.room));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.schedule, "schedule " + this.room + " tomorrow"));
        Assertions.assertThrows(CrobsException.class, () -> this.run(this.schedule, "schedule R999 2026-11-16"));
    }

    @Test
    void scheduleCannotBeUndone() {
        ScheduleCommand var10001 = this.schedule;
        Objects.requireNonNull(var10001);
        Assertions.assertThrows(CrobsException.class, var10001::undo);
        var10001 = this.schedule;
        Objects.requireNonNull(var10001);
        Assertions.assertThrows(CrobsException.class, var10001::redo);
    }

    @Test
    void removingAUserCancelsTheirBookingsAndCanBeUndone() throws CrobsException {
        this.bookAsAlice("09:00", "10:00");
        ReversibleChange<User> removal = this.userService.removeUserWithUndo(this.alice.getId());
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
        removal.undo();
        Assertions.assertEquals(1, this.bookingService.getBookingsByUser(this.alice.getId()).size());
    }

    @Test
    void deletingARoomCancelsItsFutureBookings() throws CrobsException {
        this.bookAsAlice("09:00", "10:00");
        Room deleted = (Room)this.roomService.deleteRoomWithUndo(this.room).getValue();
        Assertions.assertEquals(this.room, deleted.getId());
        Assertions.assertTrue(this.bookingService.getBookingsByUser(this.alice.getId()).isEmpty());
        Assertions.assertEquals(Role.REGULAR, this.alice.getRole());
    }
}
