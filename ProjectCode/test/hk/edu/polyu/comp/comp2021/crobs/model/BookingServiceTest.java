package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests booking creation, conflicts, modification, scheduling, and cascading cancellation. */
@SuppressWarnings("MagicNumber") // Literal hours and counts are test inputs.
class BookingServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);
    private static final String ALICE = "U001";
    private static final String BOB = "U002";
    private static final String ROOM_A = "R100";
    private static final String ROOM_B = "R200";

    private BookingRepository bookingRepository;
    private BookingService service;

    private static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    /** Creates two rooms and a service whose clock is fixed at 2026-11-12 12:00. */
    @BeforeEach
    void setUp() {
        RoomRepository rooms = new RoomRepository();
        rooms.add(new Room(ROOM_A, RoomType.MEETING_ROOM, 10, Collections.<String>emptySet()));
        rooms.add(new Room(ROOM_B, RoomType.SEMINAR_ROOM, 30, Collections.<String>emptySet()));
        bookingRepository = new BookingRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);
        service = new BookingService(bookingRepository, rooms, clock);
    }

    private Booking book(String user, String room, LocalDate date, int start, int end)
            throws CrobsException {
        return service.createBooking(user, room, date, at(start), at(end), "Meeting");
    }

    // ---- creating bookings (REQ10) ----

    /** A valid booking is stored with a generated ID. */
    @Test
    void createBookingStoresBookingWithGeneratedId() throws CrobsException {
        Booking booking = service.createBooking(ALICE, ROOM_A, MONDAY, at(9), at(10), "Study group");
        assertTrue(booking.getId().matches("B\\d{3,}"));
        assertEquals(ALICE, booking.getUserId());
        assertEquals(ROOM_A, booking.getRoomId());
        assertEquals("Study group", booking.getPurpose());
        assertSame(booking, service.getBooking(booking.getId()));
    }

    /** Every booking gets a different ID. */
    @Test
    void generatedIdsAreUnique() throws CrobsException {
        Set<String> ids = new HashSet<>();
        for (int hour = 8; hour < 12; hour++) {
            ids.add(book(ALICE, ROOM_A, MONDAY, hour, hour + 1).getId());
        }
        assertEquals(4, ids.size());
    }

    /** Partial, contained, containing, and identical overlaps are all rejected. */
    @Test
    void overlappingBookingsAreRejected() throws CrobsException {
        book(ALICE, ROOM_A, MONDAY, 10, 12);
        int[][] clashes = {{9, 11}, {11, 13}, {9, 13}, {10, 12}, {11, 12}, {10, 11}};
        for (int[] clash : clashes) {
            CrobsException error = assertThrows(CrobsException.class,
                    () -> book(BOB, ROOM_A, MONDAY, clash[0], clash[1]));
            assertTrue(error.getMessage().contains("already booked"));
        }
        assertEquals(1, bookingRepository.getAll().size());
    }

    /** Back-to-back bookings, other rooms, and other dates are allowed. */
    @Test
    void nonOverlappingBookingsAreAllowed() throws CrobsException {
        book(ALICE, ROOM_A, MONDAY, 10, 12);
        book(BOB, ROOM_A, MONDAY, 12, 13);
        book(BOB, ROOM_A, MONDAY, 9, 10);
        book(BOB, ROOM_B, MONDAY, 10, 12);
        book(BOB, ROOM_A, MONDAY.plusDays(1), 10, 12);
        assertEquals(5, bookingRepository.getAll().size());
    }

    /** Bad input is reported as an error and nothing is stored. */
    @Test
    void invalidInputIsRejected() {
        assertThrows(CrobsException.class, () -> book(ALICE, ROOM_A, MONDAY, 10, 10));
        assertThrows(CrobsException.class, () -> book(ALICE, ROOM_A, MONDAY, 11, 10));
        assertThrows(CrobsException.class, () -> book(ALICE, "R999", MONDAY, 9, 10));
        assertThrows(CrobsException.class, () -> book(ALICE, null, MONDAY, 9, 10));
        assertThrows(CrobsException.class, () -> book(" ", ROOM_A, MONDAY, 9, 10));
        assertThrows(CrobsException.class, () -> book(ALICE, ROOM_A, null, 9, 10));
        assertThrows(CrobsException.class,
                () -> service.createBooking(ALICE, ROOM_A, MONDAY, at(9), at(10), " "));
        assertThrows(CrobsException.class,
                () -> service.createBooking(ALICE, ROOM_A, MONDAY, null, at(10), "x"));
        assertTrue(bookingRepository.getAll().isEmpty());
    }

    /** The slot validator is reusable by other commands. */
    @Test
    void requireValidSlotChecksOrderAndPresence() throws CrobsException {
        BookingService.requireValidSlot(MONDAY, at(9), at(10));
        assertThrows(CrobsException.class, () -> BookingService.requireValidSlot(null, at(9), at(10)));
        assertThrows(CrobsException.class, () -> BookingService.requireValidSlot(MONDAY, at(9), at(9)));
        assertThrows(CrobsException.class, () -> BookingService.requireValidSlot(MONDAY, at(10), at(9)));
    }

    /** Availability reflects existing bookings. */
    @Test
    void isRoomAvailableReflectsBookings() throws CrobsException {
        assertTrue(service.isRoomAvailable(ROOM_A, MONDAY, at(9), at(10)));
        book(ALICE, ROOM_A, MONDAY, 9, 11);
        assertFalse(service.isRoomAvailable(ROOM_A, MONDAY, at(10), at(12)));
        assertTrue(service.isRoomAvailable(ROOM_A, MONDAY, at(11), at(12)));
        assertTrue(service.isRoomAvailable(ROOM_B, MONDAY, at(10), at(12)));
    }

    // ---- modifying bookings (REQ11) ----

    /** A modified booking keeps its ID and owner and frees its old slot. */
    @Test
    void modifyBookingChangesDetailsButKeepsIdAndOwner() throws CrobsException {
        Booking original = book(ALICE, ROOM_A, MONDAY, 9, 10);
        service.modifyBooking(ALICE, original.getId(), ROOM_B, MONDAY.plusDays(1), at(14), at(15), "New plan");
        Booking updated = service.getBooking(original.getId());
        assertEquals(original.getId(), updated.getId());
        assertEquals(ALICE, updated.getUserId());
        assertEquals(ROOM_B, updated.getRoomId());
        assertEquals(MONDAY.plusDays(1), updated.getDate());
        assertEquals(at(14), updated.getStartTime());
        assertEquals(at(15), updated.getEndTime());
        assertEquals("New plan", updated.getPurpose());
        assertTrue(service.isRoomAvailable(ROOM_A, MONDAY, at(9), at(10)));
        assertEquals(1, bookingRepository.getAll().size());
    }

    /** A booking may be changed to a slot overlapping its own old slot. */
    @Test
    void modifyBookingMayOverlapItself() throws CrobsException {
        Booking original = book(ALICE, ROOM_A, MONDAY, 9, 10);
        service.modifyBooking(ALICE, original.getId(), ROOM_A, MONDAY, at(9), at(11), "Longer");
        assertEquals(at(11), service.getBooking(original.getId()).getEndTime());
    }

    /** Moving onto another booking is rejected and changes nothing. */
    @Test
    void modifyBookingToConflictingTimeIsRejected() throws CrobsException {
        Booking mine = book(ALICE, ROOM_A, MONDAY, 9, 10);
        book(BOB, ROOM_A, MONDAY, 11, 12);
        assertThrows(CrobsException.class, () -> service.modifyBooking(
                ALICE, mine.getId(), ROOM_A, MONDAY, at(10), at(12), "Clash"));
        assertSame(mine, service.getBooking(mine.getId()));
    }

    /** Only the owner may modify, and the booking, room, and fields must be valid. */
    @Test
    void modifyBookingValidatesOwnerAndInput() throws CrobsException {
        Booking mine = book(ALICE, ROOM_A, MONDAY, 9, 10);
        String id = mine.getId();
        CrobsException notOwner = assertThrows(CrobsException.class,
                () -> service.modifyBooking(BOB, id, ROOM_A, MONDAY, at(13), at(14), "x"));
        assertTrue(notOwner.getMessage().contains("another user"));
        assertThrows(CrobsException.class,
                () -> service.modifyBooking(ALICE, "B999", ROOM_A, MONDAY, at(13), at(14), "x"));
        assertThrows(CrobsException.class,
                () -> service.modifyBooking(ALICE, null, ROOM_A, MONDAY, at(13), at(14), "x"));
        assertThrows(CrobsException.class,
                () -> service.modifyBooking(ALICE, id, "R999", MONDAY, at(13), at(14), "x"));
        assertThrows(CrobsException.class,
                () -> service.modifyBooking(ALICE, id, ROOM_A, MONDAY, at(14), at(13), "x"));
        assertThrows(CrobsException.class,
                () -> service.modifyBooking(ALICE, id, ROOM_A, MONDAY, at(13), at(14), " "));
        assertSame(mine, service.getBooking(id));
    }

    // ---- deleting bookings (REQ12) ----

    /** Deleting a booking frees its slot. */
    @Test
    void deleteBookingRemovesItAndFreesTheSlot() throws CrobsException {
        Booking mine = book(ALICE, ROOM_A, MONDAY, 9, 10);
        service.deleteBooking(ALICE, mine.getId());
        assertTrue(bookingRepository.getAll().isEmpty());
        assertTrue(service.isRoomAvailable(ROOM_A, MONDAY, at(9), at(10)));
        assertThrows(CrobsException.class, () -> service.getBooking(mine.getId()));
    }

    /** Only the owner may delete an existing booking. */
    @Test
    void deleteBookingValidatesOwnerAndExistence() throws CrobsException {
        Booking mine = book(ALICE, ROOM_A, MONDAY, 9, 10);
        assertThrows(CrobsException.class, () -> service.deleteBooking(BOB, mine.getId()));
        assertThrows(CrobsException.class, () -> service.deleteBooking(ALICE, "B999"));
        assertEquals(1, bookingRepository.getAll().size());
    }

    // ---- undo / redo (BON2) ----

    /** Undoing a creation removes the booking; redo brings back the same ID. */
    @Test
    void createBookingCanBeUndoneAndRedone() throws CrobsException {
        ReversibleChange<Booking> change =
                service.createBookingWithUndo(ALICE, ROOM_A, MONDAY, at(9), at(10), "x");
        Booking booking = change.getValue();
        change.undo();
        assertTrue(bookingRepository.getAll().isEmpty());
        change.redo();
        assertSame(booking, service.getBooking(booking.getId()));
        assertThrows(CrobsException.class, change::redo);
    }

    /** Redo of a creation fails when someone has since taken the slot. */
    @Test
    void redoOfCreationFailsWhenSlotWasTaken() throws CrobsException {
        ReversibleChange<Booking> change =
                service.createBookingWithUndo(ALICE, ROOM_A, MONDAY, at(9), at(10), "x");
        change.undo();
        book(BOB, ROOM_A, MONDAY, 9, 10);
        assertThrows(CrobsException.class, change::redo);
    }

    /** Undoing a modification restores the previous booking exactly. */
    @Test
    void modifyBookingCanBeUndoneAndRedone() throws CrobsException {
        Booking original = book(ALICE, ROOM_A, MONDAY, 9, 10);
        ReversibleChange<Booking> change = service.modifyBookingWithUndo(
                ALICE, original.getId(), ROOM_B, MONDAY, at(13), at(14), "Moved");
        change.undo();
        assertSame(original, service.getBooking(original.getId()));
        change.redo();
        assertSame(change.getValue(), service.getBooking(original.getId()));
    }

    /** Undoing a deletion restores the booking with its original ID. */
    @Test
    void deleteBookingCanBeUndoneAndRedone() throws CrobsException {
        Booking original = book(ALICE, ROOM_A, MONDAY, 9, 10);
        ReversibleChange<Booking> change = service.deleteBookingWithUndo(ALICE, original.getId());
        change.undo();
        assertSame(original, service.getBooking(original.getId()));
        change.redo();
        assertTrue(bookingRepository.getAll().isEmpty());
    }

    // ---- weekly schedule (REQ14) ----

    /** Only the seven days from the start date are listed, sorted by date and time. */
    @Test
    void scheduleCoversSevenDaysSortedAndForOneRoom() throws CrobsException {
        book(ALICE, ROOM_A, MONDAY.minusDays(1), 9, 10);
        book(ALICE, ROOM_A, MONDAY.plusDays(7), 9, 10);
        book(ALICE, ROOM_B, MONDAY, 9, 10);
        Booking tuesday = book(ALICE, ROOM_A, MONDAY.plusDays(1), 10, 11);
        Booking mondayLate = book(ALICE, ROOM_A, MONDAY, 14, 15);
        Booking mondayEarly = book(ALICE, ROOM_A, MONDAY, 9, 10);
        Booking lastDay = book(ALICE, ROOM_A, MONDAY.plusDays(6), 9, 10);
        List<ScheduleEntry> schedule = service.getSchedule(ROOM_A, MONDAY, ALICE, Role.REGULAR);
        assertEquals(4, schedule.size());
        assertSame(mondayEarly, schedule.get(0).getBooking());
        assertSame(mondayLate, schedule.get(1).getBooking());
        assertSame(tuesday, schedule.get(2).getBooking());
        assertSame(lastDay, schedule.get(3).getBooking());
        assertEquals(MONDAY.plusDays(6), BookingService.lastDayOfWeek(MONDAY));
    }

    /** Regular users see only their own bookings' owners; administrators see all. */
    @Test
    void scheduleHidesOtherUsersFromRegularUsers() throws CrobsException {
        book(ALICE, ROOM_A, MONDAY, 9, 10);
        book(BOB, ROOM_A, MONDAY, 10, 11);
        List<ScheduleEntry> aliceView = service.getSchedule(ROOM_A, MONDAY, ALICE, Role.REGULAR);
        assertEquals(2, aliceView.size());
        assertEquals(ALICE, aliceView.get(0).getVisibleUserId().get());
        assertFalse(aliceView.get(1).getVisibleUserId().isPresent());
        for (Role role : new Role[] {Role.ADMIN, Role.SUPERADMIN}) {
            for (ScheduleEntry entry : service.getSchedule(ROOM_A, MONDAY, "U999", role)) {
                assertTrue(entry.isOwnerVisible());
            }
        }
        assertFalse(service.getSchedule(ROOM_A, MONDAY, ALICE, null).get(1).isOwnerVisible());
    }

    /** An unknown room or missing date is an error; an empty week is an empty list. */
    @Test
    void scheduleValidatesInput() throws CrobsException {
        assertThrows(CrobsException.class,
                () -> service.getSchedule("R999", MONDAY, ALICE, Role.REGULAR));
        assertThrows(CrobsException.class,
                () -> service.getSchedule(ROOM_A, null, ALICE, Role.REGULAR));
        assertTrue(service.getSchedule(ROOM_A, MONDAY, ALICE, Role.REGULAR).isEmpty());
    }

    // ---- listing a user's bookings ----

    /** A user's bookings are sorted by date and start time and exclude other users'. */
    @Test
    void getBookingsByUserIsSortedAndFiltered() throws CrobsException {
        Booking later = book(ALICE, ROOM_A, MONDAY.plusDays(2), 9, 10);
        Booking earlyTuesday = book(ALICE, ROOM_B, MONDAY.plusDays(1), 8, 9);
        Booking lateTuesday = book(ALICE, ROOM_A, MONDAY.plusDays(1), 15, 16);
        book(BOB, ROOM_A, MONDAY, 9, 10);
        List<Booking> mine = service.getBookingsByUser(ALICE);
        assertEquals(3, mine.size());
        assertSame(earlyTuesday, mine.get(0));
        assertSame(lateTuesday, mine.get(1));
        assertSame(later, mine.get(2));
        assertTrue(service.getBookingsByUser("U999").isEmpty());
    }

    // ---- cascading cancellation (REQ3, REQ9) ----

    /** Cancelling a user's bookings leaves everyone else's, and can be undone and redone. */
    @Test
    void cancelBookingsByUserIsReversible() throws CrobsException {
        Booking a1 = book(ALICE, ROOM_A, MONDAY, 9, 10);
        Booking a2 = book(ALICE, ROOM_B, MONDAY, 9, 10);
        Booking b1 = book(BOB, ROOM_A, MONDAY, 10, 11);
        ReversibleChange<Void> change = service.cancelBookingsByUser(ALICE);
        assertEquals(1, bookingRepository.getAll().size());
        assertSame(b1, service.getBooking(b1.getId()));
        change.undo();
        assertSame(a1, service.getBooking(a1.getId()));
        assertSame(a2, service.getBooking(a2.getId()));
        change.redo();
        assertEquals(1, bookingRepository.getAll().size());
    }

    /** Cancelling for a user with no bookings still returns an undo record. */
    @Test
    void cancelBookingsByUserWithNoBookingsIsANoOp() throws CrobsException {
        book(BOB, ROOM_A, MONDAY, 9, 10);
        ReversibleChange<Void> change = service.cancelBookingsByUser(ALICE);
        assertNotNull(change);
        change.undo();
        assertEquals(1, bookingRepository.getAll().size());
    }

    /** Only future bookings of the room are cancelled; past and other rooms' bookings stay. */
    @Test
    void cancelFutureBookingsForRoomKeepsPastBookings() throws CrobsException {
        Booking past = book(ALICE, ROOM_A, LocalDate.of(2026, 11, 1), 9, 10);
        Booking earlierToday = book(ALICE, ROOM_A, LocalDate.of(2026, 11, 12), 9, 10);
        Booking laterToday = book(ALICE, ROOM_A, LocalDate.of(2026, 11, 12), 13, 14);
        Booking future = book(BOB, ROOM_A, LocalDate.of(2026, 11, 20), 9, 10);
        Booking otherRoom = book(BOB, ROOM_B, LocalDate.of(2026, 11, 20), 9, 10);
        ReversibleChange<Void> change = service.cancelFutureBookingsForRoom(ROOM_A);
        assertEquals(3, bookingRepository.getAll().size());
        assertSame(past, service.getBooking(past.getId()));
        assertSame(earlierToday, service.getBooking(earlierToday.getId()));
        assertSame(otherRoom, service.getBooking(otherRoom.getId()));
        change.undo();
        assertSame(laterToday, service.getBooking(laterToday.getId()));
        assertSame(future, service.getBooking(future.getId()));
        change.redo();
        assertEquals(3, bookingRepository.getAll().size());
    }

    /** Undoing a cancellation fails, without partial changes, when an ID is already back in use. */
    @Test
    void undoOfCancellationFailsWhenIdIsInUse() throws CrobsException {
        book(ALICE, ROOM_A, MONDAY, 9, 10);
        Booking second = book(ALICE, ROOM_A, MONDAY, 11, 12);
        ReversibleChange<Void> change = service.cancelBookingsByUser(ALICE);
        bookingRepository.add(new Booking(second.getId(), BOB, ROOM_B, MONDAY,
                at(9), at(10), "Impostor"));
        assertThrows(CrobsException.class, change::undo);
        assertEquals(1, bookingRepository.getAll().size());
    }

    /** Redoing a cancellation fails when a cancelled booking was replaced meanwhile. */
    @Test
    void redoOfCancellationFailsWhenBookingChanged() throws CrobsException {
        Booking first = book(ALICE, ROOM_A, MONDAY, 9, 10);
        ReversibleChange<Void> change = service.cancelBookingsByUser(ALICE);
        change.undo();
        bookingRepository.remove(first.getId());
        bookingRepository.add(new Booking(first.getId(), ALICE, ROOM_A, MONDAY, at(9), at(10), "Copy"));
        assertThrows(CrobsException.class, change::redo);
    }

    // ---- construction ----

    /** Required collaborators must not be null, and the system-clock constructor works. */
    @Test
    void constructorsRequireCollaborators() throws CrobsException {
        RoomRepository rooms = new RoomRepository();
        assertThrows(NullPointerException.class, () -> new BookingService(null, rooms));
        assertThrows(NullPointerException.class, () -> new BookingService(bookingRepository, null));
        assertThrows(NullPointerException.class,
                () -> new BookingService(bookingRepository, rooms, null));
        BookingService systemClock = new BookingService(bookingRepository, rooms);
        assertNotNull(systemClock.cancelFutureBookingsForRoom(ROOM_A));
    }
}
