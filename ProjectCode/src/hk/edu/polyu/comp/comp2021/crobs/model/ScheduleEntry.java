package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Objects;
import java.util.Optional;

/** One row of a room schedule; the owner's user ID is only revealed to permitted viewers. */
public final class ScheduleEntry {
    private final Booking booking;
    private final boolean ownerVisible;

    /**
     * Creates a schedule row.
     *
     * @param booking the booking shown in this row
     * @param ownerVisible whether the viewer may see who owns the booking
     */
    public ScheduleEntry(Booking booking, boolean ownerVisible) {
        this.booking = Objects.requireNonNull(booking, "Booking is required.");
        this.ownerVisible = ownerVisible;
    }

    /** @return the booking shown in this row */
    public Booking getBooking() {
        return booking;
    }

    /** @return whether the viewer may see the owner of this booking */
    public boolean isOwnerVisible() {
        return ownerVisible;
    }

    /** @return the owner's user ID when visible, otherwise an empty result */
    public Optional<String> getVisibleUserId() {
        return ownerVisible ? Optional.of(booking.getUserId()) : Optional.empty();
    }
}
