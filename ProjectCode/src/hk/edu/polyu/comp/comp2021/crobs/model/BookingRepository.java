package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory storage of bookings with constant-time lookups by ID. */
public class BookingRepository {
    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    /**
     * Stores a booking without replacing an existing identity.
     *
     * @param booking the booking to add
     */
    public void add(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking is required.");
        }
        if (bookings.containsKey(booking.getId())) {
            throw new IllegalArgumentException("Booking ID already exists: " + booking.getId());
        }
        bookings.put(booking.getId(), booking);
    }

    /**
     * Looks up a booking.
     *
     * @param id the booking identifier
     * @return the stored booking, or null when absent
     */
    public Booking get(String id) {
        return bookings.get(id);
    }

    /**
     * Removes a booking.
     *
     * @param id the booking identifier
     * @return the removed booking, or null when absent
     */
    public Booking remove(String id) {
        return bookings.remove(id);
    }

    /** @return a detached list of stored bookings in insertion order */
    public List<Booking> getAll() {
        return new ArrayList<>(bookings.values());
    }
}
