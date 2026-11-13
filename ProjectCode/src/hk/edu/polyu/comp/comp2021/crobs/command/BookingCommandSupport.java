package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;

/** Parsing and formatting shared by Member B's booking commands. */
final class BookingCommandSupport {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private BookingCommandSupport() {
    }

    /**
     * Parses a date written as yyyy-MM-dd.
     *
     * @param text the command argument
     * @return the date
     * @throws CrobsException when the text is not a real calendar date in that format
     */
    static LocalDate parseDate(String text) throws CrobsException {
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException invalid) {
            throw new CrobsException("Invalid date '" + text + "'. Use yyyy-MM-dd, e.g. 2026-11-16.");
        }
    }

    /**
     * Parses a time written as HH:mm.
     *
     * @param text the command argument
     * @return the time
     * @throws CrobsException when the text is not a real time in that format
     */
    static LocalTime parseTime(String text) throws CrobsException {
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException invalid) {
            throw new CrobsException("Invalid time '" + text + "'. Use HH:mm, e.g. 09:30.");
        }
    }

    /**
     * Joins the trailing tokens of a command into one text, so a purpose may contain spaces.
     *
     * @param args the dispatcher tokens
     * @param firstIndex the index of the first token to include
     * @return the tokens joined by single spaces
     */
    static String joinFrom(String[] args, int firstIndex) {
        return String.join(" ", Arrays.copyOfRange(args, firstIndex, args.length));
    }

    /**
     * Describes a booking on one line.
     *
     * @param booking the booking
     * @return a readable summary
     */
    static String describe(Booking booking) {
        return String.format("%s: room %s on %s from %s to %s (%s)", booking.getId(),
                booking.getRoomId(), booking.getDate(), booking.getStartTime(),
                booking.getEndTime(), booking.getPurpose());
    }
}
