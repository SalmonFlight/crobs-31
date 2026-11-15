package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the RoomService. */
@SuppressWarnings("MagicNumber")
class RoomServiceTest {
    private RoomRepository repository;
    private FakeBookingCancellation bookings;
    private RoomService service;

    @BeforeEach
    void setUp() {
        repository = new RoomRepository();
        bookings = new FakeBookingCancellation();
        service = new RoomService(repository, bookings);
    }

    private Room addRoom() throws CrobsException {
        return service.addRoom(RoomType.MEETING_ROOM, 12,
                new LinkedHashSet<>(Arrays.asList("projector", "whiteboard")));
    }

    @Test
    void requiresBothRepositoryAndBookingAdapter() {
        assertThrows(NullPointerException.class, () -> new RoomService(null, bookings));
        assertThrows(NullPointerException.class, () -> new RoomService(repository, null));
    }

    @Test
    void createsRoomsWithUniqueIdsAndCopiesTheirFeatures() throws CrobsException {
        Set<String> features = new HashSet<>(Collections.singleton("projector"));
        Room first = service.addRoom(RoomType.COMPUTER_LAB, 30, features);
        Room second = addRoom();
        features.clear();
        assertTrue(first.getId().matches("R\\d{3,}"));
        assertNotEquals(first.getId(), second.getId());
        assertEquals(RoomType.COMPUTER_LAB, first.getType());
        assertEquals(30, first.getCapacity());
        assertEquals(Collections.singleton("projector"), first.getFeatures());
        assertThrows(UnsupportedOperationException.class, () -> first.getFeatures().clear());
        assertSame(first, service.getRoom(first.getId()));
    }

    @Test
    void rejectsInvalidRoomDescriptionsWithoutCreatingRooms() {
        assertThrows(CrobsException.class, () -> service.addRoom(null, 10, Collections.emptySet()));
        assertThrows(CrobsException.class, () -> service.addRoom(RoomType.MEETING_ROOM, 0, Collections.emptySet()));
        assertThrows(CrobsException.class, () -> service.addRoom(RoomType.MEETING_ROOM, -1, Collections.emptySet()));
        assertThrows(CrobsException.class, () -> service.addRoom(RoomType.MEETING_ROOM, 10, null));
        assertThrows(CrobsException.class, () -> service.addRoom(RoomType.MEETING_ROOM, 10, Collections.singleton(" ")));
        assertThrows(CrobsException.class, () -> service.addRoom(RoomType.MEETING_ROOM, 10, Collections.singleton(null)));
        assertTrue(service.getAllRooms().isEmpty());
    }

    @Test
    void acceptsAnEmptyFeatureSet() throws CrobsException {
        Room room = service.addRoom(RoomType.LECTURE_THEATRE, 100, Collections.emptySet());
        assertTrue(room.getFeatures().isEmpty());
    }

    @Test
    void featureParsingSupportsNoFeaturesAndRejectsMalformedLists() throws CrobsException {
        assertEquals(Collections.emptySet(), RoomService.parseFeatures("-"));
        assertEquals(new HashSet<>(Arrays.asList("screen", "projector")),
                RoomService.parseFeatures("screen,projector,screen"));
        for (String invalid : Arrays.asList(null, "", ",screen", "screen,", "screen,,projector", "screen projector", "screen,\tprojector")) {
            assertThrows(CrobsException.class, () -> RoomService.parseFeatures(invalid));
        }
    }

    @Test
    void modifiesOnlyTheRequestedFieldWithoutMutatingPreviousSnapshots() throws CrobsException {
        Room original = addRoom();
        service.modifyRoom(original.getId(), "capacity", "24");
        Room larger = service.getRoom(original.getId());
        assertEquals(24, larger.getCapacity());
        assertEquals(original.getType(), larger.getType());
        assertEquals(original.getFeatures(), larger.getFeatures());
        assertEquals(12, original.getCapacity());

        service.modifyRoom(original.getId(), "roomType", "seminarRoom");
        assertEquals(RoomType.SEMINAR_ROOM, service.getRoom(original.getId()).getType());
        assertEquals(24, service.getRoom(original.getId()).getCapacity());

        service.modifyRoom(original.getId(), "features", "screen,whiteboard");
        Room revised = service.getRoom(original.getId());
        assertEquals(new HashSet<>(Arrays.asList("screen", "whiteboard")), revised.getFeatures());
        assertEquals(RoomType.SEMINAR_ROOM, revised.getType());
        assertEquals(original.getId(), revised.getId());
    }

    @Test
    void rejectsMalformedUpdatesWithoutChangingTheRoom() throws CrobsException {
        Room room = addRoom();
        String[][] invalid = {
                {"capacity", "0"}, {"capacity", "-1"}, {"capacity", "many"},
                {"capacity", "99999999999999999999"}, {"roomType", "office"},
                {"features", "projector,,whiteboard"}, {"features", "projector, whiteboard"},
                {"id", "R999"}, {"unknown", "value"}, {null, "value"}, {"capacity", null}
        };
        for (String[] update : invalid) {
            assertThrows(CrobsException.class,
                    () -> service.modifyRoom(room.getId(), update[0], update[1]), Arrays.toString(update));
            assertSame(room, service.getRoom(room.getId()), "An invalid change must be atomic.");
        }
        assertThrows(CrobsException.class, () -> service.modifyRoom("missing", "capacity", "10"));
    }

    @Test
    void roomListsAreSortedAndDetachedFromRepository() {
        repository.add(new Room("R020", RoomType.COMPUTER_LAB, 20, Collections.emptySet()));
        repository.add(new Room("R003", RoomType.SEMINAR_ROOM, 30, Collections.emptySet()));
        repository.add(new Room("R001", RoomType.MEETING_ROOM, 10, Collections.emptySet()));
        List<Room> rooms = service.getAllRooms();
        assertEquals(Arrays.asList("R001", "R003", "R020"),
                rooms.stream().map(Room::getId).collect(Collectors.toList()));
        rooms.clear();
        assertEquals(3, repository.getAll().size());
    }

    @Test
    void deletionCancelsFutureBookingsAndPreservesHistoryAndOtherRooms() throws CrobsException {
        Room room = addRoom();
        bookings.add("past", "U001", room.getId(), FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(1));
        bookings.add("other", "U001", "R-other", FakeBookingCancellation.NOW.plusDays(1));

        service.deleteRoom(room.getId());

        assertNull(repository.get(room.getId()));
        assertTrue(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
        assertTrue(bookings.contains("other"));
        assertEquals(1, bookings.roomCancellations);
    }

    @Test
    void missingRoomErrorsDoNotInvokeBookingCancellation() {
        assertThrows(CrobsException.class, () -> service.getRoom("missing"));
        assertThrows(CrobsException.class, () -> service.deleteRoom("missing"));
        assertEquals(0, bookings.roomCancellations);
    }

    @Test
    void failedCascadeKeepsTheRoomAndBookings() throws CrobsException {
        Room room = addRoom();
        bookings.add("future", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(1));
        bookings.failCancellation = true;

        assertThrows(CrobsException.class, () -> service.deleteRoom(room.getId()));

        assertSame(room, service.getRoom(room.getId()));
        assertTrue(bookings.contains("future"));
    }

    @Test
    void missingCancellationRecordDoesNotDeleteRoom() throws CrobsException {
        Room room = addRoom();
        bookings.returnNullCancellation = true;
        assertThrows(CrobsException.class, () -> service.deleteRoom(room.getId()));
        assertSame(room, service.getRoom(room.getId()));
    }

    @Test
    void failedBookingRestorationRollsBackRoomRestorationAndCanBeRetried() throws CrobsException {
        Room room = addRoom();
        bookings.add("future", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(1));
        ReversibleChange<Room> deletion = service.deleteRoomWithUndo(room.getId());
        bookings.failUndo = true;
        assertThrows(CrobsException.class, deletion::undo);
        assertNull(repository.get(room.getId()));
        assertFalse(bookings.contains("future"));
        bookings.failUndo = false;
        deletion.undo();
        assertSame(room, service.getRoom(room.getId()));
        assertTrue(bookings.contains("future"));
        bookings.failRedo = true;
        assertThrows(CrobsException.class, deletion::redo);
        assertSame(room, service.getRoom(room.getId()));
        assertTrue(bookings.contains("future"));
        bookings.failRedo = false;
        deletion.redo();
        assertNull(repository.get(room.getId()));
        assertFalse(bookings.contains("future"));
    }

    @Test
    void undoAndRedoRoomAdditionReuseTheSameIdentity() throws CrobsException {
        ReversibleChange<Room> change = service.addRoomWithUndo(RoomType.SEMINAR_ROOM, 20, Collections.emptySet());
        Room room = change.getValue();
        assertSame(room, service.getRoom(room.getId()));
        change.undo();
        assertNull(repository.get(room.getId()));
        change.redo();
        assertSame(room, service.getRoom(room.getId()));
        assertNotEquals(room.getId(), addRoom().getId());
    }

    @Test
    void undoAndRedoRestoreAllRoomFields() throws CrobsException {
        Room original = addRoom();
        ReversibleChange<Room> change = service.modifyRoomWithUndo(original.getId(), "features", "screen");
        assertEquals(Collections.singleton("screen"), service.getRoom(original.getId()).getFeatures());
        change.undo();
        assertSame(original, service.getRoom(original.getId()));
        change.redo();
        Room revised = service.getRoom(original.getId());
        assertEquals(Collections.singleton("screen"), revised.getFeatures());
        assertEquals(original.getType(), revised.getType());
        assertEquals(original.getCapacity(), revised.getCapacity());
    }

    @Test
    void undoAndRedoDeletionRestoreItsExactFutureBookingSnapshot() throws CrobsException {
        Room room = addRoom();
        bookings.add("past", "U001", room.getId(), FakeBookingCancellation.NOW.minusDays(1));
        bookings.add("future", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(1));
        ReversibleChange<Room> change = service.deleteRoomWithUndo(room.getId());
        assertSame(room, change.getValue());
        assertTrue(bookings.contains("past"));
        assertFalse(bookings.contains("future"));

        change.undo();
        assertSame(room, service.getRoom(room.getId()));
        assertTrue(bookings.contains("past"));
        assertTrue(bookings.contains("future"));
        bookings.add("later", "U001", room.getId(), FakeBookingCancellation.NOW.plusDays(2));

        change.redo();
        assertNull(repository.get(room.getId()));
        assertTrue(bookings.contains("past"));
        assertFalse(bookings.contains("future"));
        assertTrue(bookings.contains("later"));
        assertEquals(1, bookings.roomCancellations);
    }
}