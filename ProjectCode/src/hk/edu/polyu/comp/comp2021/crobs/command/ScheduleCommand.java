package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Shows a room's bookings for one week (REQ14). Not undoable. */
public final class ScheduleCommand implements Command {
    private static final int ARGUMENT_COUNT = 3;
    private static final int DATE_ARGUMENT_INDEX = 2;
    private static final String HIDDEN_OWNER = "hidden";
    private final BookingService bookingService;

    /**
     * Constructs the command.
     *
     * @param bookingService service managing bookings
     */
    public ScheduleCommand(BookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "schedule";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        User user = session.requireLoggedIn();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "schedule roomId weekStartDate");
        LocalDate weekStart = BookingCommandSupport.parseDate(args[DATE_ARGUMENT_INDEX]);
        List<ScheduleEntry> entries =
                bookingService.getSchedule(args[1], weekStart, user.getId(), user.getRole());
        String period = weekStart + " to " + BookingService.lastDayOfWeek(weekStart);
        if (entries.isEmpty()) {
            return CommandResult.success(
                    "No bookings for room " + args[1] + " from " + period + ".", entries);
        }
        StringBuilder text = new StringBuilder("Schedule for room " + args[1] + ", " + period + ":");
        for (ScheduleEntry entry : entries) {
            Booking booking = entry.getBooking();
            text.append(String.format("%n%s | %s | %s-%s | user %s | %s", booking.getId(),
                    booking.getDate(), booking.getStartTime(), booking.getEndTime(),
                    entry.getVisibleUserId().orElse(HIDDEN_OWNER), booking.getPurpose()));
        }
        return CommandResult.success(text.toString(), entries);
    }

    /** {@inheritDoc} */
    @Override
    public void undo() throws CrobsException {
        throw new CrobsException("schedule cannot be undone.");
    }

    /** {@inheritDoc} */
    @Override
    public void redo() throws CrobsException {
        throw new CrobsException("schedule cannot be redone.");
    }
}
