package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.List;

/**
 * Command to list all bookings made by the current user.
 */
public class ListBookingsCommand implements Command {
    private final BookingService bookingService;

    /**
     * Constructs a ListBookingsCommand.
     *
     * @param bookingService the booking service
     */
    public ListBookingsCommand(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "listBookings"; }

    /**
     * Executes the command.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the command result
     * @throws CrobsException if an error occurs
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        User currentUser = session.requireLoggedIn();
        List<Booking> bookings = bookingService.getBookingsByUser(currentUser.getId());

        if (bookings.isEmpty()) {
            return CommandResult.success("You have no bookings.", null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Your Bookings:\n");
        for (Booking b : bookings) {
            sb.append(String.format("ID: %s | Room: %s | Date: %s | Time: %s-%s | Purpose: %s\n",
                    b.getId(), b.getRoomId(), b.getDate(), b.getStartTime(), b.getEndTime(), b.getPurpose()));
        }

        return CommandResult.success(sb.toString(), bookings);
    }

    @Override
    public void undo() throws CrobsException {}

    @Override
    public void redo() throws CrobsException {}
}