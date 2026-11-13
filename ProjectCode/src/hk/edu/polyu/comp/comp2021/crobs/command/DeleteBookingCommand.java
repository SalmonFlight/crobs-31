package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.Objects;

/** Deletes a booking owned by the logged-in user (REQ12). */
public final class DeleteBookingCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 2;
    private final BookingService bookingService;

    /**
     * Constructs the command.
     *
     * @param bookingService service managing bookings
     */
    public DeleteBookingCommand(BookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "deleteBooking";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        User user = session.requireLoggedIn();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "deleteBooking bookingID");
        Booking booking = remember(bookingService.deleteBookingWithUndo(user.getId(), args[1]));
        return CommandResult.success("Deleted booking " + booking.getId() + ".", booking);
    }
}
