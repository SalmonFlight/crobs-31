package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Creates a booking for the logged-in user (REQ10). */
public final class BookCommand extends AbstractUndoableCommand {
    private static final String USAGE = "book roomId date startTime endTime purpose";
    private static final int MINIMUM_ARGUMENT_COUNT = 6;
    private static final int DATE_ARGUMENT_INDEX = 2;
    private static final int START_ARGUMENT_INDEX = 3;
    private static final int END_ARGUMENT_INDEX = 4;
    private static final int PURPOSE_ARGUMENT_INDEX = 5;
    private final BookingService bookingService;

    /**
     * Constructs the command.
     *
     * @param bookingService service managing bookings
     */
    public BookCommand(BookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "book";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        User user = session.requireLoggedIn();
        CommandArguments.requireMinimum(args, getName(), MINIMUM_ARGUMENT_COUNT, USAGE);
        LocalDate date = BookingCommandSupport.parseDate(args[DATE_ARGUMENT_INDEX]);
        LocalTime start = BookingCommandSupport.parseTime(args[START_ARGUMENT_INDEX]);
        LocalTime end = BookingCommandSupport.parseTime(args[END_ARGUMENT_INDEX]);
        String purpose = BookingCommandSupport.joinFrom(args, PURPOSE_ARGUMENT_INDEX);
        Booking booking = remember(bookingService.createBookingWithUndo(
                user.getId(), args[1], date, start, end, purpose));
        return CommandResult.success("Booked " + BookingCommandSupport.describe(booking), booking);
    }
}
