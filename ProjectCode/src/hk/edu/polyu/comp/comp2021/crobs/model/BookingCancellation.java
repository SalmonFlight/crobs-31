package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;

/**
 * Connection to Member B's booking domain for cascading deletion and BON2.
 * Implementations cancel atomically and return a non-null change whose undo
 * restores exactly the removed bookings and whose redo removes those bookings.
 * No booking storage, scheduling, or cancellation policy is implemented here.
 */
public interface BookingCancellation {
    /**
     * Cancels all bookings of a removed user.
     * @param userId the user being removed
     * @return an already-applied cancellation, including when no bookings match
     * @throws CrobsException when cancellation cannot complete without partial changes
     */
    ReversibleChange<Void> cancelBookingsByUser(String userId) throws CrobsException;

    /**
     * Cancels future bookings of a removed room, preserving past bookings.
     * @param roomId the room being removed
     * @return an already-applied cancellation, including when no bookings match
     * @throws CrobsException when cancellation cannot complete without partial changes
     */
    ReversibleChange<Void> cancelFutureBookingsForRoom(String roomId) throws CrobsException;
}
