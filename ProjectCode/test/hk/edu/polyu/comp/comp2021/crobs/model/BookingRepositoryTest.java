package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests booking storage and the schedule row wrapper. */
class BookingRepositoryTest {
    private BookingRepository repository;

    private static Booking booking(String id) {
        return new Booking(id, "U001", "R100", LocalDate.of(2026, 11, 16),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Study");
    }

    /** Starts every test with an empty repository. */
    @BeforeEach
    void setUp() {
        repository = new BookingRepository();
    }

    /** Added bookings can be found and listed in insertion order. */
    @Test
    void addGetAndGetAllWork() {
        Booking first = booking("B1");
        Booking second = booking("B2");
        repository.add(first);
        repository.add(second);
        assertSame(first, repository.get("B1"));
        assertEquals(2, repository.getAll().size());
        assertSame(first, repository.getAll().get(0));
    }

    /** Null and duplicate bookings are refused. */
    @Test
    void addRejectsNullAndDuplicates() {
        repository.add(booking("B1"));
        assertThrows(IllegalArgumentException.class, () -> repository.add(null));
        assertThrows(IllegalArgumentException.class, () -> repository.add(booking("B1")));
    }

    /** Remove returns the booking once, then null. */
    @Test
    void removeReturnsTheRemovedBookingOnce() {
        Booking first = booking("B1");
        repository.add(first);
        assertSame(first, repository.remove("B1"));
        assertNull(repository.remove("B1"));
        assertNull(repository.get("B1"));
    }

    /** Changing the returned list does not change the repository. */
    @Test
    void getAllReturnsADetachedList() {
        repository.add(booking("B1"));
        List<Booking> copy = repository.getAll();
        copy.clear();
        assertEquals(1, repository.getAll().size());
    }

    /** A schedule row reveals the owner only when allowed. */
    @Test
    void scheduleEntryHidesOwnerWhenNotVisible() {
        Booking booking = booking("B1");
        ScheduleEntry visible = new ScheduleEntry(booking, true);
        ScheduleEntry hidden = new ScheduleEntry(booking, false);
        assertSame(booking, visible.getBooking());
        assertTrue(visible.isOwnerVisible());
        assertEquals(Optional.of("U001"), visible.getVisibleUserId());
        assertFalse(hidden.isOwnerVisible());
        assertFalse(hidden.getVisibleUserId().isPresent());
        assertThrows(NullPointerException.class, () -> new ScheduleEntry(null, true));
    }
}
