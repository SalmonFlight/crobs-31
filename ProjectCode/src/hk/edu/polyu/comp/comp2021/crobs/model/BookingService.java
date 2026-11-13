package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.util.IdGenerator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Booking validation, conflict detection, scheduling, and reversible cancellation. */
public class BookingService implements BookingCancellation {
    /** Number of days shown by a weekly schedule. */
    public static final int DAYS_IN_WEEK = 7;

    private static final Comparator<Booking> CHRONOLOGICAL = Comparator.comparing(Booking::getDate)
            .thenComparing(Booking::getStartTime)
            .thenComparing(Booking::getId);

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final Clock clock;

    /**
     * Creates a booking service that uses the system clock to decide what is "future".
     *
     * @param bookings booking storage
     * @param rooms room storage, used only to check that booked rooms exist
     */
    public BookingService(BookingRepository bookings, RoomRepository rooms) {
        this(bookings, rooms, Clock.systemDefaultZone());
    }

    /**
     * Creates a booking service with an explicit clock, so tests can fix the current time.
     *
     * @param bookings booking storage
     * @param rooms room storage, used only to check that booked rooms exist
     * @param clock the source of the current time
     */
    public BookingService(BookingRepository bookings, RoomRepository rooms, Clock clock) {
        this.bookings = Objects.requireNonNull(bookings, "Booking repository is required.");
        this.rooms = Objects.requireNonNull(rooms, "Room repository is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    /**
     * Gets the last day of the 7-day period that starts on a given date.
     *
     * @param weekStart the first day of the period
     * @return the last day of the period
     */
    public static LocalDate lastDayOfWeek(LocalDate weekStart) {
        return weekStart.plusDays(DAYS_IN_WEEK - 1);
    }

    /**
     * Checks that a date and time interval is complete and that the start is before the end.
     *
     * @param date the date
     * @param start the start time
     * @param end the end time
     * @throws CrobsException when the slot is incomplete or reversed
     */
    public static void requireValidSlot(LocalDate date, LocalTime start, LocalTime end)
            throws CrobsException {
        if (date == null || start == null || end == null) {
            throw new CrobsException("Date, start time, and end time are required.");
        }
        if (!start.isBefore(end)) {
            throw new CrobsException("Start time must be before end time.");
        }
    }

    /**
     * Creates a booking for a user.
     *
     * @param userId the ID of the booking owner
     * @param roomId the ID of the room to book
     * @param date the booking date
     * @param start the start time
     * @param end the end time
     * @param purpose the purpose of the booking
     * @return the new booking
     * @throws CrobsException when a field is invalid, the room is missing, or the slot is taken
     */
    public Booking createBooking(String userId, String roomId, LocalDate date,
                                 LocalTime start, LocalTime end, String purpose)
            throws CrobsException {
        return createBookingWithUndo(userId, roomId, date, start, end, purpose).getValue();
    }

    /**
     * Creates a booking and records its inverse operation.
     *
     * @param userId the ID of the booking owner
     * @param roomId the ID of the room to book
     * @param date the booking date
     * @param start the start time
     * @param end the end time
     * @param purpose the purpose of the booking
     * @return the applied creation
     * @throws CrobsException when a field is invalid, the room is missing, or the slot is taken
     */
    public ReversibleChange<Booking> createBookingWithUndo(String userId, String roomId,
                                                           LocalDate date, LocalTime start,
                                                           LocalTime end, String purpose)
            throws CrobsException {
        requireText(userId, "User ID");
        requireText(purpose, "Purpose");
        requireRoom(roomId);
        requireValidSlot(date, start, end);
        requireAvailable(roomId, date, start, end, null);
        Booking booking = new Booking(nextId(), userId, roomId, date, start, end, purpose);
        bookings.add(booking);
        return ReversibleChange.applied(booking, () -> {
            requireCurrent(booking);
            bookings.remove(booking.getId());
        }, () -> {
            requireVacant(booking.getId());
            requireRoom(booking.getRoomId());
            requireAvailable(booking.getRoomId(), booking.getDate(),
                    booking.getStartTime(), booking.getEndTime(), null);
            bookings.add(booking);
        });
    }

    /**
     * Modifies a booking owned by the given user.
     *
     * @param currentUserId the ID of the user making the change
     * @param bookingId the booking to modify
     * @param roomId the new room ID
     * @param date the new date
     * @param start the new start time
     * @param end the new end time
     * @param purpose the new purpose
     * @throws CrobsException when validation fails or the new slot conflicts
     */
    public void modifyBooking(String currentUserId, String bookingId, String roomId,
                              LocalDate date, LocalTime start, LocalTime end, String purpose)
            throws CrobsException {
        modifyBookingWithUndo(currentUserId, bookingId, roomId, date, start, end, purpose);
    }

    /**
     * Modifies a booking and records its complete previous state. The booking ID and owner
     * never change, and the booking does not conflict with itself.
     *
     * @param currentUserId the ID of the user making the change
     * @param bookingId the booking to modify
     * @param roomId the new room ID
     * @param date the new date
     * @param start the new start time
     * @param end the new end time
     * @param purpose the new purpose
     * @return the applied modification, whose value is the updated booking
     * @throws CrobsException when validation fails or the new slot conflicts
     */
    public ReversibleChange<Booking> modifyBookingWithUndo(String currentUserId, String bookingId,
                                                           String roomId, LocalDate date,
                                                           LocalTime start, LocalTime end,
                                                           String purpose)
            throws CrobsException {
        Booking previous = getOwnedBooking(currentUserId, bookingId);
        requireText(purpose, "Purpose");
        requireRoom(roomId);
        requireValidSlot(date, start, end);
        requireAvailable(roomId, date, start, end, bookingId);
        Booking replacement = new Booking(previous.getId(), previous.getUserId(),
                roomId, date, start, end, purpose);
        replace(previous, replacement);
        return ReversibleChange.applied(replacement,
                () -> replace(replacement, previous), () -> replace(previous, replacement));
    }

    /**
     * Deletes a booking owned by the given user.
     *
     * @param currentUserId the ID of the user making the change
     * @param bookingId the booking to delete
     * @throws CrobsException when the booking is missing or owned by someone else
     */
    public void deleteBooking(String currentUserId, String bookingId) throws CrobsException {
        deleteBookingWithUndo(currentUserId, bookingId);
    }

    /**
     * Deletes a booking and records its inverse operation.
     *
     * @param currentUserId the ID of the user making the change
     * @param bookingId the booking to delete
     * @return the applied deletion, whose value is the deleted booking
     * @throws CrobsException when the booking is missing or owned by someone else
     */
    public ReversibleChange<Booking> deleteBookingWithUndo(String currentUserId, String bookingId)
            throws CrobsException {
        Booking booking = getOwnedBooking(currentUserId, bookingId);
        bookings.remove(bookingId);
        return ReversibleChange.applied(booking, () -> {
            requireVacant(booking.getId());
            bookings.add(booking);
        }, () -> {
            requireCurrent(booking);
            bookings.remove(booking.getId());
        });
    }

    /**
     * Finds an existing booking.
     *
     * @param bookingId the booking ID
     * @return the booking
     * @throws CrobsException when the booking does not exist
     */
    public Booking getBooking(String bookingId) throws CrobsException {
        Booking booking = bookingId == null ? null : bookings.get(bookingId);
        if (booking == null) {
            throw new CrobsException("Booking not found: " + bookingId);
        }
        return booking;
    }

    /**
     * Checks whether a room is free for a time slot. The slot is assumed to be valid; use
     * {@link #requireValidSlot(LocalDate, LocalTime, LocalTime)} to validate user input first.
     *
     * @param roomId the room ID
     * @param date the date
     * @param start the start time
     * @param end the end time
     * @return true when no existing booking overlaps the slot
     */
    public boolean isRoomAvailable(String roomId, LocalDate date, LocalTime start, LocalTime end) {
        return findConflict(roomId, date, start, end, null) == null;
    }

    /**
     * Lists a room's bookings in the 7-day period starting at a date. Administrators see the
     * owner of every booking; other users see only their own bookings' owner.
     *
     * @param roomId the room ID
     * @param weekStart the first day of the period
     * @param currentUserId the ID of the viewing user
     * @param role the role of the viewing user
     * @return the schedule rows sorted by date, then start time
     * @throws CrobsException when the room does not exist or the date is missing
     */
    public List<ScheduleEntry> getSchedule(String roomId, LocalDate weekStart,
                                           String currentUserId, Role role)
            throws CrobsException {
        requireRoom(roomId);
        if (weekStart == null) {
            throw new CrobsException("Week start date is required.");
        }
        LocalDate lastDay = lastDayOfWeek(weekStart);
        boolean seesEveryOwner = role == Role.ADMIN || role == Role.SUPERADMIN;
        List<Booking> inPeriod = new ArrayList<>();
        for (Booking booking : bookings.getAll()) {
            if (booking.getRoomId().equals(roomId)
                    && !booking.getDate().isBefore(weekStart)
                    && !booking.getDate().isAfter(lastDay)) {
                inPeriod.add(booking);
            }
        }
        inPeriod.sort(CHRONOLOGICAL);
        List<ScheduleEntry> entries = new ArrayList<>();
        for (Booking booking : inPeriod) {
            boolean visible = seesEveryOwner || booking.getUserId().equals(currentUserId);
            entries.add(new ScheduleEntry(booking, visible));
        }
        return entries;
    }

    /**
     * Lists every booking owned by a user.
     *
     * @param userId the user ID
     * @return the user's bookings sorted by date, then start time
     */
    public List<Booking> getBookingsByUser(String userId) {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings.getAll()) {
            if (booking.getUserId().equals(userId)) {
                result.add(booking);
            }
        }
        result.sort(CHRONOLOGICAL);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public ReversibleChange<Void> cancelBookingsByUser(String userId) {
        return cancelMatching(booking -> booking.getUserId().equals(userId));
    }

    /** {@inheritDoc} */
    @Override
    public ReversibleChange<Void> cancelFutureBookingsForRoom(String roomId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return cancelMatching(booking -> booking.getRoomId().equals(roomId)
                && booking.getStartDateTime().isAfter(now));
    }

    private ReversibleChange<Void> cancelMatching(Predicate<Booking> matches) {
        List<Booking> removed = new ArrayList<>();
        for (Booking booking : bookings.getAll()) {
            if (matches.test(booking)) {
                removed.add(booking);
            }
        }
        for (Booking booking : removed) {
            bookings.remove(booking.getId());
        }
        return ReversibleChange.applied(null, () -> restore(removed), () -> discard(removed));
    }

    private void restore(List<Booking> removed) throws CrobsException {
        for (Booking booking : removed) {
            requireVacant(booking.getId());
        }
        for (Booking booking : removed) {
            bookings.add(booking);
        }
    }

    private void discard(List<Booking> removed) throws CrobsException {
        for (Booking booking : removed) {
            requireCurrent(booking);
        }
        for (Booking booking : removed) {
            bookings.remove(booking.getId());
        }
    }

    private Booking getOwnedBooking(String currentUserId, String bookingId) throws CrobsException {
        Booking booking = getBooking(bookingId);
        if (!booking.getUserId().equals(currentUserId)) {
            throw new CrobsException("Booking " + bookingId + " belongs to another user.");
        }
        return booking;
    }

    private Booking findConflict(String roomId, LocalDate date, LocalTime start, LocalTime end,
                                 String ignoredBookingId) {
        for (Booking existing : bookings.getAll()) {
            if (!existing.getId().equals(ignoredBookingId)
                    && existing.conflictsWith(roomId, date, start, end)) {
                return existing;
            }
        }
        return null;
    }

    private void requireAvailable(String roomId, LocalDate date, LocalTime start, LocalTime end,
                                  String ignoredBookingId) throws CrobsException {
        Booking conflict = findConflict(roomId, date, start, end, ignoredBookingId);
        if (conflict != null) {
            throw new CrobsException("Room " + roomId + " is already booked on " + date
                    + " from " + conflict.getStartTime() + " to " + conflict.getEndTime()
                    + " (booking " + conflict.getId() + ").");
        }
    }

    private void requireRoom(String roomId) throws CrobsException {
        if (roomId == null || rooms.get(roomId) == null) {
            throw new CrobsException("Room not found: " + roomId);
        }
    }

    private static void requireText(String value, String label) throws CrobsException {
        if (value == null || value.trim().isEmpty()) {
            throw new CrobsException(label + " must not be blank.");
        }
    }

    private String nextId() {
        String id;
        do {
            id = IdGenerator.generateBookingId();
        } while (bookings.get(id) != null);
        return id;
    }

    private void requireCurrent(Booking expected) throws CrobsException {
        if (bookings.get(expected.getId()) != expected) {
            throw new CrobsException("Booking changed since this operation: " + expected.getId());
        }
    }

    private void requireVacant(String bookingId) throws CrobsException {
        if (bookings.get(bookingId) != null) {
            throw new CrobsException("Booking ID is already in use: " + bookingId);
        }
    }

    private void replace(Booking expected, Booking replacement) throws CrobsException {
        requireCurrent(expected);
        bookings.remove(expected.getId());
        bookings.add(replacement);
    }
}
