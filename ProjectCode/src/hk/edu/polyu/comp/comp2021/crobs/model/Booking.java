package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/** An immutable room booking, identified by its booking ID. */
public final class Booking {
    private final String id;
    private final String userId;
    private final String roomId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;

    /**
     * Creates a booking after validating every field.
     *
     * @param id the nonblank booking identifier
     * @param userId the nonblank ID of the owning user
     * @param roomId the nonblank ID of the booked room
     * @param date the booking date
     * @param startTime the start time, which must be before the end time
     * @param endTime the end time
     * @param purpose the nonblank purpose of the booking
     */
    public Booking(String id, String userId, String roomId, LocalDate date,
                   LocalTime startTime, LocalTime endTime, String purpose) {
        requireText(id, "Booking ID");
        requireText(userId, "User ID");
        requireText(roomId, "Room ID");
        requireText(purpose, "Purpose");
        if (date == null) {
            throw new IllegalArgumentException("Booking date is required.");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
    }

    /** @return the booking identifier */
    public String getId() {
        return id;
    }

    /** @return the ID of the user who owns this booking */
    public String getUserId() {
        return userId;
    }

    /** @return the ID of the booked room */
    public String getRoomId() {
        return roomId;
    }

    /** @return the booking date */
    public LocalDate getDate() {
        return date;
    }

    /** @return the start time */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** @return the end time */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** @return the purpose of the booking */
    public String getPurpose() {
        return purpose;
    }

    /** @return the moment this booking starts */
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(date, startTime);
    }

    /**
     * Tests whether this booking conflicts with a proposed slot: same room, same date,
     * and overlapping time intervals. Back-to-back intervals do not overlap.
     *
     * @param otherRoomId the proposed room
     * @param otherDate the proposed date
     * @param otherStart the proposed start time
     * @param otherEnd the proposed end time
     * @return whether the proposed slot clashes with this booking
     */
    public boolean conflictsWith(String otherRoomId, LocalDate otherDate,
                                 LocalTime otherStart, LocalTime otherEnd) {
        return roomId.equals(otherRoomId)
                && date.equals(otherDate)
                && startTime.isBefore(otherEnd)
                && otherStart.isBefore(endTime);
    }

    /**
     * Compares bookings by persistent identity.
     *
     * @param other the object to compare
     * @return whether both objects represent the same booking
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Booking)) {
            return false;
        }
        Booking booking = (Booking) other;
        return id.equals(booking.getId());
    }

    /** @return the hash code of the booking identifier */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
