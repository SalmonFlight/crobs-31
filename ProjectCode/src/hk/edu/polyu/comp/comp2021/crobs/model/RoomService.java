package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.util.IdGenerator;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Room validation, updates, and reversible cascading deletion. */
public class RoomService {
    private final RoomRepository rooms;
    private final BookingCancellation bookings;

    /**
     * Creates a room service sharing the application's room repository.
     * @param rooms room storage
     * @param bookings Member B's reversible booking cancellation adapter
     */
    public RoomService(RoomRepository rooms, BookingCancellation bookings) {
        this.rooms = Objects.requireNonNull(rooms, "Room repository is required.");
        this.bookings = Objects.requireNonNull(bookings, "Booking cancellation is required.");
    }

    /**
     * Adds a room with a generated ID.
     * @param type the room type
     * @param capacity positive room capacity
     * @param features nonblank feature names
     * @return the new room
     * @throws CrobsException when a field is invalid
     */
    public Room addRoom(RoomType type, int capacity, Set<String> features) throws CrobsException {
        return addRoomWithUndo(type, capacity, features).getValue();
    }

    /**
     * Adds a room and records its inverse operation.
     * @param type the room type
     * @param capacity positive room capacity
     * @param features nonblank feature names
     * @return the applied creation
     * @throws CrobsException when a field is invalid
     */
    public ReversibleChange<Room> addRoomWithUndo(RoomType type, int capacity, Set<String> features)
            throws CrobsException {
        validate(type, capacity, features);
        Room room = new Room(nextId(), type, capacity, features);
        rooms.add(room);
        return ReversibleChange.applied(room, () -> {
            requireCurrent(room);
            rooms.remove(room.getId());
        }, () -> {
            requireVacant(room.getId());
            rooms.add(room);
        });
    }

    /**
     * Updates one room field while preserving its ID.
     * @param roomId the room ID
     * @param field roomType (or type), capacity, or features
     * @param newValue the new field value
     * @throws CrobsException when the room or value is invalid
     */
    public void modifyRoom(String roomId, String field, String newValue) throws CrobsException {
        modifyRoomWithUndo(roomId, field, newValue);
    }

    /**
     * Updates one room field and records the complete previous state.
     * @param roomId the room ID
     * @param field roomType (or type), capacity, or features
     * @param newValue the new field value
     * @return the applied update
     * @throws CrobsException when validation fails
     */
    public ReversibleChange<Room> modifyRoomWithUndo(String roomId, String field, String newValue)
            throws CrobsException {
        Room previous = getRoom(roomId);
        if (field == null || newValue == null) {
            throw new CrobsException("Room field and value are required.");
        }
        RoomType type = previous.getType();
        int capacity = previous.getCapacity();
        Set<String> features = previous.getFeatures();
        try {
            switch (field) {
                case "roomType":
                case "type":
                    type = RoomType.fromCommandValue(newValue);
                    break;
                case "capacity":
                    capacity = Integer.parseInt(newValue);
                    break;
                case "features":
                    features = parseFeatures(newValue);
                    break;
                default:
                    throw new CrobsException("Room field must be roomType, capacity, or features.");
            }
        } catch (IllegalArgumentException invalidValue) {
            throw new CrobsException("Invalid room " + field + ": " + newValue);
        }
        validate(type, capacity, features);
        Room replacement = new Room(previous.getId(), type, capacity, features);
        replace(previous, replacement);
        return ReversibleChange.applied(replacement,
                () -> replace(replacement, previous), () -> replace(previous, replacement));
    }

    /**
     * Deletes a room and cancels only its future bookings.
     * @param roomId the room ID
     * @throws CrobsException when the room is missing or cancellation fails
     */
    public void deleteRoom(String roomId) throws CrobsException {
        deleteRoomWithUndo(roomId);
    }

    /**
     * Deletes a room with a reversible booking cascade.
     * @param roomId the room ID
     * @return the applied deletion
     * @throws CrobsException when validation or cancellation fails
     */
    public ReversibleChange<Room> deleteRoomWithUndo(String roomId) throws CrobsException {
        Room room = getRoom(roomId);
        ReversibleChange<Void> cancellation = bookings.cancelFutureBookingsForRoom(roomId);
        if (cancellation == null) {
            throw new CrobsException("Booking cancellation did not provide an undo record.");
        }
        rooms.remove(roomId);
        return ReversibleChange.applied(room, () -> {
            requireVacant(room.getId());
            rooms.add(room);
            try {
                cancellation.undo();
            } catch (CrobsException failure) {
                rooms.remove(room.getId());
                throw failure;
            }
        }, () -> {
            requireCurrent(room);
            cancellation.redo();
            rooms.remove(room.getId());
        });
    }

    /**
     * Finds an existing room.
     * @param roomId the room ID
     * @return the room
     * @throws CrobsException when the room does not exist
     */
    public Room getRoom(String roomId) throws CrobsException {
        Room room = roomId == null ? null : rooms.get(roomId);
        if (room == null) {
            throw new CrobsException("Room not found: " + roomId);
        }
        return room;
    }

    /**
     * Gets a detached room list sorted by ID.
     * @return all rooms
     */
    public List<Room> getAllRooms() {
        List<Room> result = rooms.getAll();
        result.sort(Comparator.comparing(Room::getId));
        return result;
    }

    /**
     * Parses comma-separated feature names; '-' represents no features.
     * @param value the command argument
     * @return the feature set
     * @throws CrobsException when an element is blank or contains whitespace
     */
    public static Set<String> parseFeatures(String value) throws CrobsException {
        if ("-".equals(value)) {
            return Collections.emptySet();
        }
        if (value == null || value.isEmpty()) {
            throw new CrobsException("Features must be comma-separated names, or '-' for none.");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String feature : value.split(",", -1)) {
            if (feature.isEmpty() || feature.chars().anyMatch(Character::isWhitespace)) {
                throw new CrobsException("Features must be comma-separated without spaces.");
            }
            result.add(feature);
        }
        return result;
    }

    private static void validate(RoomType type, int capacity, Set<String> features) throws CrobsException {
        if (type == null || capacity <= 0 || features == null) {
            throw new CrobsException("Room type, positive capacity, and features are required.");
        }
        for (String feature : features) {
            if (feature == null || feature.isEmpty()
                    || feature.indexOf(',') >= 0 || feature.chars().anyMatch(Character::isWhitespace)) {
                throw new CrobsException("Each room feature must be one nonblank name without commas or spaces.");
            }
        }
    }

    private String nextId() {
        String id;
        do {
            id = IdGenerator.generateRoomId();
        } while (rooms.get(id) != null);
        return id;
    }

    private void requireCurrent(Room expected) throws CrobsException {
        if (rooms.get(expected.getId()) != expected) {
            throw new CrobsException("Room changed since this operation: " + expected.getId());
        }
    }

    private void requireVacant(String roomId) throws CrobsException {
        if (rooms.get(roomId) != null) {
            throw new CrobsException("Room ID is already in use: " + roomId);
        }
    }

    private void replace(Room expected, Room replacement) throws CrobsException {
        requireCurrent(expected);
        rooms.remove(expected.getId());
        rooms.add(replacement);
    }
}
