package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory storage of rooms with constant-time lookups by ID. */
public class RoomRepository {
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    /**
     * Stores a room without replacing an existing identity.
     *
     * @param room the room to add
     */
    public void add(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room is required.");
        }
        if (rooms.containsKey(room.getId())) {
            throw new IllegalArgumentException("Room ID already exists: " + room.getId());
        }
        rooms.put(room.getId(), room);
    }

    /**
     * Looks up a room.
     *
     * @param id the room identifier
     * @return the stored room, or null when absent
     */
    public Room get(String id) {
        return rooms.get(id);
    }

    /**
     * Removes a room.
     *
     * @param id the room identifier
     * @return the removed room, or null when absent
     */
    public Room remove(String id) {
        return rooms.remove(id);
    }

    /** @return a detached list of stored rooms in insertion order */
    public List<Room> getAll() {
        return new ArrayList<>(rooms.values());
    }
}
