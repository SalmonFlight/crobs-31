package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** A booking-domain test double; does not implement Member B's production code. */
final class FakeBookingCancellation implements BookingCancellation {
    static final LocalDateTime NOW = LocalDateTime.of(2030, 3, 15, 12, 0);

    // These fields do NOT have the word 'private', so tests can access them directly.
    boolean failCancellation;
    boolean failUndo;
    boolean failRedo;
    boolean returnNullCancellation;
    int userCancellations;
    int roomCancellations;

    private final Map<String, Entry> bookings = new LinkedHashMap<>();

    // I removed the setters and getters because they were unused.

    void add(String id, String userId, String roomId, LocalDateTime start) {
        bookings.put(id, new Entry(id, userId, roomId, start));
    }

    boolean contains(String id) {
        return bookings.containsKey(id);
    }

    @Override
    public ReversibleChange<Void> cancelBookingsByUser(String userId) throws CrobsException {
        userCancellations++;
        if (returnNullCancellation) {
            return null;
        }
        return cancel(entry -> entry.getUserId().equals(userId));
    }

    @Override
    public ReversibleChange<Void> cancelFutureBookingsForRoom(String roomId) throws CrobsException {
        roomCancellations++;
        if (returnNullCancellation) {
            return null;
        }
        return cancel(entry -> entry.getRoomId().equals(roomId) && entry.getStart().isAfter(NOW));
    }

    private ReversibleChange<Void> cancel(Predicate<Entry> matches) throws CrobsException {
        if (failCancellation) {
            throw new CrobsException("Simulated booking cancellation failure.");
        }
        List<Entry> removed = new ArrayList<>();
        for (Entry entry : bookings.values()) {
            if (matches.test(entry)) {
                removed.add(entry);
            }
        }
        remove(removed);
        return ReversibleChange.applied(null, () -> {
            if (failUndo) {
                throw new CrobsException("Simulated booking restore failure.");
            }
            removed.forEach(entry -> bookings.put(entry.getId(), entry));
        }, () -> {
            if (failRedo) {
                throw new CrobsException("Simulated booking redo failure.");
            }
            remove(removed);
        });
    }

    private void remove(List<Entry> entries) {
        entries.forEach(entry -> bookings.remove(entry.getId()));
    }

    private static final class Entry {
        private final String id;
        private final String userId;
        private final String roomId;
        private final LocalDateTime start;

        private Entry(String id, String userId, String roomId, LocalDateTime start) {
            this.id = id;
            this.userId = userId;
            this.roomId = roomId;
            this.start = start;
        }

        private String getId() { return id; }
        private String getUserId() { return userId; }
        private String getRoomId() { return roomId; }
        private LocalDateTime getStart() { return start; }
    }
}