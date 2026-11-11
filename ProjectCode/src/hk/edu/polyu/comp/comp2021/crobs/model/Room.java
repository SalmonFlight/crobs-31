package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** An immutable room description, identified by its room ID. */
public final class Room {
    private final String id;
    private final RoomType type;
    private final int capacity;
    private final Set<String> features;

    /**
     * Creates a room with a defensive copy of its features.
     *
     * @param id the nonblank room identifier
     * @param type the room category
     * @param capacity the positive seating capacity
     * @param features the room features, which may be empty
     */
    public Room(String id, RoomType type, int capacity, Set<String> features) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID must not be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Room type is required.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive.");
        }
        if (features == null) {
            throw new IllegalArgumentException("Room features are required.");
        }
        for (String feature : features) {
            if (feature == null || feature.trim().isEmpty()) {
                throw new IllegalArgumentException("Room features must not be blank.");
            }
        }
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.features = Collections.unmodifiableSet(new LinkedHashSet<>(features));
    }

    /** @return the room identifier */
    public String getId() {
        return id;
    }

    /** @return the room category */
    public RoomType getType() {
        return type;
    }

    /** @return the seating capacity */
    public int getCapacity() {
        return capacity;
    }

    /** @return an unmodifiable set of room features */
    public Set<String> getFeatures() {
        return features;
    }

    /**
     * Compares rooms by persistent identity.
     *
     * @param other the object to compare
     * @return whether both objects represent the same room
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Room)) {
            return false;
        }
        Room room = (Room) other;
        return id.equals(room.getId());
    }

    /** @return the hash code of the room identifier */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
