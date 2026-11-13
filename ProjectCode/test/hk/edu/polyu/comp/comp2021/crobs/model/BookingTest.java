package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the Booking entity's validation, identity, and conflict rule. */
class BookingTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);

    private static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    private static Booking booking(String id, int startHour, int endHour) {
        return new Booking(id, "U001", "R100", DATE, at(startHour), at(endHour), "Study");
    }

    /** A valid booking exposes every field it was created with. */
    @Test
    void gettersReturnConstructorValues() {
        Booking booking = booking("B001", 9, 11);
        assertEquals("B001", booking.getId());
        assertEquals("U001", booking.getUserId());
        assertEquals("R100", booking.getRoomId());
        assertEquals(DATE, booking.getDate());
        assertEquals(at(9), booking.getStartTime());
        assertEquals(at(11), booking.getEndTime());
        assertEquals("Study", booking.getPurpose());
        assertEquals(LocalDateTime.of(DATE, at(9)), booking.getStartDateTime());
    }

    /** Every required field is validated. */
    @Test
    void invalidFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Booking(null, "U001", "R100", DATE, at(9), at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking(" ", "U001", "R100", DATE, at(9), at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "", "R100", DATE, at(9), at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U001", null, DATE, at(9), at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U001", "R100", DATE, at(9), at(10), " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U001", "R100", null, at(9), at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U001", "R100", DATE, null, at(10), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U001", "R100", DATE, at(9), null, "x"));
        assertThrows(IllegalArgumentException.class, () -> booking("B1", 10, 10));
        assertThrows(IllegalArgumentException.class, () -> booking("B1", 11, 10));
    }

    /** Bookings are the same booking when their IDs match. */
    @Test
    void equalityFollowsTheBookingId() {
        Booking first = booking("B001", 9, 10);
        Booking sameIdDifferentSlot = booking("B001", 13, 14);
        assertEquals(first, first);
        assertEquals(first, sameIdDifferentSlot);
        assertEquals(first.hashCode(), sameIdDifferentSlot.hashCode());
        assertNotEquals(first, booking("B002", 9, 10));
        assertNotEquals(first, null);
        assertNotEquals(first, "B001");
    }

    /** Overlapping intervals conflict; back-to-back intervals do not. */
    @Test
    void conflictsWithDetectsOverlapOnly() {
        Booking existing = booking("B001", 10, 12);
        assertTrue(existing.conflictsWith("R100", DATE, at(10), at(12)));
        assertTrue(existing.conflictsWith("R100", DATE, at(9), at(11)));
        assertTrue(existing.conflictsWith("R100", DATE, at(11), at(13)));
        assertTrue(existing.conflictsWith("R100", DATE, at(9), at(13)));
        assertTrue(existing.conflictsWith("R100", DATE, at(11), at(12)));
        assertFalse(existing.conflictsWith("R100", DATE, at(9), at(10)));
        assertFalse(existing.conflictsWith("R100", DATE, at(12), at(13)));
        assertFalse(existing.conflictsWith("R200", DATE, at(10), at(12)));
        assertFalse(existing.conflictsWith("R100", DATE.plusDays(1), at(10), at(12)));
    }
}
